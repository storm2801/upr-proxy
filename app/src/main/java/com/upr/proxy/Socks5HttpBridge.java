package com.upr.proxy;

import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor SOCKS5 local que reenvía todas las conexiones al proxy HTTP
 * corporativo usando CONNECT + Proxy-Authorization.
 *
 * Actualiza ProxyStatus en tiempo real y cierra agresivamente todos los
 * sockets abiertos en stop() para que la desconexión sea inmediata.
 */
public class Socks5HttpBridge {

    private static final String TAG = "Socks5HttpBridge";
    private static final int LISTEN_PORT = 1080;
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT_MS = 15000;
    /** Reintentos ante 503 Service Unavailable (transitorio por congestión del proxy). */
    private static final int MAX_RETRIES_503 = 2;
    private static final long RETRY_BACKOFF_MS = 400;

    private final String httpProxyHost;
    private final int httpProxyPort;
    private final String authHeader;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<Socket> openSockets =
            Collections.synchronizedSet(new HashSet<Socket>());

    public Socks5HttpBridge(String host, int port, String user, String pass) {
        this.httpProxyHost = host;
        this.httpProxyPort = port;
        if (user != null && !user.isEmpty()) {
            String cred = user + ":" + (pass == null ? "" : pass);
            this.authHeader = "Basic " + Base64.encodeToString(
                    cred.getBytes(), Base64.NO_WRAP);
        } else {
            this.authHeader = null;
        }
    }

    public void start() throws IOException {
        if (running.get()) return;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("127.0.0.1", LISTEN_PORT));
        running.set(true);

        ProxyStatus.reset();
        ProxyStatus.serverHost = httpProxyHost;
        ProxyStatus.serverPort = httpProxyPort;
        ProxyStatus.startedAt = System.currentTimeMillis();

        acceptThread = new Thread(this::acceptLoop, "socks5-accept");
        acceptThread.start();
        L.i(TAG, "Puente SOCKS5→HTTP en 127.0.0.1:" + LISTEN_PORT
                + " → " + httpProxyHost + ":" + httpProxyPort);
    }

    public void stop() {
        running.set(false);

        // 1. Cerrar server socket → desbloquea accept()
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            L.w(TAG, "Error cerrando serverSocket", e);
        }

        // 2. Cerrar TODOS los sockets abiertos
        synchronized (openSockets) {
            for (Socket s : openSockets) {
                try { s.close(); } catch (IOException ignored) {}
            }
            openSockets.clear();
        }

        // 3. Interrumpir hilos
        workers.shutdownNow();
        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        ProxyStatus.vpnRunning.set(false);
        ProxyStatus.activeConnections.set(0);
        L.i(TAG, "Puente SOCKS5→HTTP detenido");
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                openSockets.add(client);
                workers.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running.get()) {
                    L.w(TAG, "Error aceptando conexión", e);
                }
            }
        }
    }

    private void handleClient(Socket client) {
        boolean counted = false;
        try {
            client.setTcpNoDelay(true);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            // ---- Handshake SOCKS5 ----
            byte[] hello = new byte[2];
            if (!readFully(in, hello)) return;
            if ((hello[0] & 0xFF) != 0x05) return;
            int nMethods = hello[1] & 0xFF;
            byte[] methods = new byte[nMethods];
            if (!readFully(in, methods)) return;

            out.write(new byte[]{0x05, 0x00});
            out.flush();

            // ---- Petición ----
            byte[] req = new byte[4];
            if (!readFully(in, req)) return;
            if ((req[0] & 0xFF) != 0x05) return;
            int cmd = req[1] & 0xFF;
            int atyp = req[3] & 0xFF;

            if (cmd != 0x01) {
                sendSocksReply(out, 0x07);
                return;
            }

            String host;
            if (atyp == 0x01) {
                byte[] a = new byte[4];
                if (!readFully(in, a)) return;
                host = (a[0] & 0xFF) + "." + (a[1] & 0xFF) + "."
                        + (a[2] & 0xFF) + "." + (a[3] & 0xFF);
            } else if (atyp == 0x03) {
                byte[] lenBuf = new byte[1];
                if (!readFully(in, lenBuf)) return;
                int len = lenBuf[0] & 0xFF;
                byte[] d = new byte[len];
                if (!readFully(in, d)) return;
                host = new String(d, java.nio.charset.StandardCharsets.UTF_8);
            } else if (atyp == 0x04) {
                byte[] a = new byte[16];
                if (!readFully(in, a)) return;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 16; i += 2) {
                    if (i > 0) sb.append(":");
                    sb.append(Integer.toHexString(((a[i] & 0xFF) << 8) | (a[i + 1] & 0xFF)));
                }
                host = sb.toString();
            } else {
                sendSocksReply(out, 0x08);
                return;
            }

            byte[] portBuf = new byte[2];
            if (!readFully(in, portBuf)) return;
            int destPort = ((portBuf[0] & 0xFF) << 8) | (portBuf[1] & 0xFF);

            // L.d(TAG, "CONNECT → " + host + ":" + destPort);

            // ---- Conectar al proxy HTTP (con reintentos ante 503) ----
            // El proxy de la UPR (squid) a veces devuelve 503 Service Unavailable
            // de forma transitoria bajo carga. Reintentar la conexión resuelve
            // la mayoría de los fallos sin afectar al usuario.
            int attempt = 0;
            boolean conectado = false;
            while (attempt <= MAX_RETRIES_503 && !conectado) {
                Socket proxy = new Socket();
                openSockets.add(proxy);
                try {
                    proxy.connect(new InetSocketAddress(httpProxyHost, httpProxyPort),
                            CONNECT_TIMEOUT_MS);
                    proxy.setTcpNoDelay(true);

                    OutputStream pOut = proxy.getOutputStream();
                    InputStream pIn = proxy.getInputStream();

                    String formattedHost = host;
                    if (formattedHost != null && formattedHost.contains(":")
                            && !formattedHost.startsWith("[")) {
                        formattedHost = "[" + formattedHost + "]";
                    }

                    // Usamos encabezados estándar de un navegador Chrome para intentar superar el 403
                    sb.append("CONNECT ").append(formattedHost).append(":").append(destPort)
                            .append(" HTTP/1.1\r\n");
                    sb.append("Host: ").append(formattedHost).append(":").append(destPort).append("\r\n");
                    sb.append("Proxy-Connection: keep-alive\r\n");
                    sb.append("Connection: keep-alive\r\n");
                    sb.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\r\n");
                    if (authHeader != null) {
                        sb.append("Proxy-Authorization: ").append(authHeader).append("\r\n");
                    }
                    sb.append("\r\n");
                    byte[] reqBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    pOut.write(reqBytes);
                    pOut.flush();
                    ProxyStatus.bytesSent.addAndGet(reqBytes.length);

                    String statusLine = readLine(pIn);
                    if (statusLine == null || !statusLine.contains(" 200 ")) {
                        // Consumir cabeceras para no dejar el socket colgado
                        String hdr;
                        while ((hdr = readLine(pIn)) != null && !hdr.isEmpty()) { /* ignorar */ }

                        if (statusLine != null && statusLine.contains(" 503 ")
                                && attempt < MAX_RETRIES_503) {
                            L.w(TAG, "Proxy 503 para " + host + ":" + destPort
                                    + " (intento " + (attempt + 1) + "/" + MAX_RETRIES_503
                                    + ") → reintentando");
                            openSockets.remove(proxy);
                            try { proxy.close(); } catch (IOException ignored) {}
                            attempt++;
                            try {
                                Thread.sleep(RETRY_BACKOFF_MS * attempt);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        }
                        L.w(TAG, "Proxy rechazó CONNECT: " + statusLine);
                        sendSocksReply(out, 0x05);
                        return;
                    }
                    String line;
                    while ((line = readLine(pIn)) != null && !line.isEmpty()) { /* ignorar */ }

                    // L.i(TAG, "✅ Proxy aceptó CONNECT → " + host + ":" + destPort);
                    sendSocksReply(out, 0x00);

                    ProxyStatus.activeConnections.incrementAndGet();
                    ProxyStatus.totalConnections.incrementAndGet();
                    counted = true;

                    // Tubería bidireccional EN PARALELO.
                    // Los dos pipes DEBEN correr en hilos separados.
                    final java.net.Socket fClient = client;
                    final java.net.Socket fProxy = proxy;

                    Thread upPipe = new Thread(() -> {
                        try {
                            pipe(in, pOut, false);
                        } finally {
                            try { fProxy.shutdownOutput(); } catch (Exception ignored) {}
                        }
                    }, "socks-up");

                    Thread downPipe = new Thread(() -> {
                        try {
                            pipe(pIn, out, true);
                        } finally {
                            try { fClient.shutdownOutput(); } catch (Exception ignored) {}
                        }
                    }, "socks-down");

                    upPipe.start();
                    downPipe.start();
                    try {
                        upPipe.join();
                        downPipe.join();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    conectado = true;

                } catch (IOException e) {
                    L.w(TAG, "Error conectando al proxy: " + e.getMessage());
                    try { sendSocksReply(out, 0x05); } catch (IOException ignored) {}
                    openSockets.remove(proxy);
                    try { proxy.close(); } catch (IOException ignored) {}
                    return;
                } finally {
                    if (!conectado) {
                        openSockets.remove(proxy);
                        try { proxy.close(); } catch (IOException ignored) {}
                    }
                }
            }

        } catch (IOException e) {
            L.w(TAG, "Error atendiendo cliente", e);
        } finally {
            openSockets.remove(client);
            try { client.close(); } catch (IOException ignored) {}
            if (counted) {
                ProxyStatus.activeConnections.decrementAndGet();
            }
        }
    }

    private void sendSocksReply(OutputStream out, int rep) throws IOException {
        out.write(new byte[]{
                0x05, (byte) rep, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00
        });
        out.flush();
    }

    private boolean readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) return false;
            off += n;
        }
        return true;
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
            if (sb.length() > 8192) break;
        }
        if (c == -1 && sb.length() == 0) return null;
        return sb.toString();
    }

    /** Copia bytes hasta EOF. No cierra sockets: eso lo hace handleClient. */
    private void pipe(InputStream src, OutputStream dst, boolean isDownstream) {
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            int n;
            while ((n = src.read(buf)) != -1) {
                dst.write(buf, 0, n);
                dst.flush();
                if (isDownstream) {
                    ProxyStatus.bytesReceived.addAndGet(n);
                } else {
                    ProxyStatus.bytesSent.addAndGet(n);
                }
            }
        } catch (IOException ignored) {
            // normal al cerrar la conexión
        }
    }
}
