package com.upr.proxy;

import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor SOCKS5 local que reenvía todas las conexiones al proxy HTTP corporativo
 * usando el método CONNECT con autenticación básica.
 *
 * Flujo:
 *   1. Escucha en 127.0.0.1:1080
 *   2. Recibe handshake SOCKS5 sin autenticación
 *   3. Recibe petición CONNECT host:puerto
 *   4. Abre TCP al proxy HTTP, envía "CONNECT host:puerto HTTP/1.1"
 *      con cabecera Proxy-Authorization: Basic <user:pass>
 *   5. Tras el 200 OK, responde SOCKS5 success y hace de tubería bidireccional
 */
public class Socks5HttpBridge {

    private static final String TAG = "Socks5HttpBridge";
    private static final int LISTEN_PORT = 1080;
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT_MS = 15000;

    private final String httpProxyHost;
    private final int httpProxyPort;
    private final String authHeader;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);

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

        acceptThread = new Thread(this::acceptLoop, "socks5-accept");
        acceptThread.start();
        L.i(TAG, "Puente SOCKS5→HTTP escuchando en 127.0.0.1:" + LISTEN_PORT
                + " → " + httpProxyHost + ":" + httpProxyPort);
    }

    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            L.w(TAG, "Error cerrando serverSocket", e);
        }
        workers.shutdownNow();
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        L.i(TAG, "Puente SOCKS5→HTTP detenido");
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                workers.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (running.get()) {
                    L.w(TAG, "Error aceptando conexión", e);
                }
            }
        }
    }

    private void handleClient(Socket client) {
        try {
            client.setTcpNoDelay(true);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            // --- Handshake SOCKS5 ---
            // [VER=5][NMETHODS][METHODS...]
            byte[] hello = new byte[2];
            if (!readFully(in, hello)) return;
            if ((hello[0] & 0xFF) != 0x05) return;
            int nMethods = hello[1] & 0xFF;
            byte[] methods = new byte[nMethods];
            if (!readFully(in, methods)) return;

            // Método 0x00 = sin autenticación
            out.write(new byte[]{0x05, 0x00});
            out.flush();

            // --- Petición SOCKS5 ---
            // [VER=5][CMD][RSV=0][ATYP][ADDR][PORT]
            byte[] req = new byte[4];
            if (!readFully(in, req)) return;
            if ((req[0] & 0xFF) != 0x05) return;
            int cmd = req[1] & 0xFF;
            int atyp = req[3] & 0xFF;

            if (cmd != 0x01) { // solo CONNECT
                sendSocksReply(out, 0x07);
                return;
            }

            String host;
            if (atyp == 0x01) { // IPv4
                byte[] a = new byte[4];
                if (!readFully(in, a)) return;
                host = (a[0] & 0xFF) + "." + (a[1] & 0xFF) + "." + (a[2] & 0xFF) + "." + (a[3] & 0xFF);
            } else if (atyp == 0x03) { // Dominio
                byte[] lenBuf = new byte[1];
                if (!readFully(in, lenBuf)) return;
                int len = lenBuf[0] & 0xFF;
                byte[] d = new byte[len];
                if (!readFully(in, d)) return;
                host = new String(d);
            } else if (atyp == 0x04) { // IPv6
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

            L.d(TAG, "CONNECT → " + host + ":" + destPort);

            // --- Conectar al proxy HTTP ---
            Socket proxy = new Socket();
            try {
                proxy.connect(new InetSocketAddress(httpProxyHost, httpProxyPort), CONNECT_TIMEOUT_MS);
                proxy.setTcpNoDelay(true);

                OutputStream pOut = proxy.getOutputStream();
                InputStream pIn = proxy.getInputStream();

                // Enviar CONNECT al proxy HTTP
                StringBuilder sb = new StringBuilder();
                sb.append("CONNECT ").append(host).append(":").append(destPort)
                        .append(" HTTP/1.1\r\n");
                sb.append("Host: ").append(host).append(":").append(destPort).append("\r\n");
                if (authHeader != null) {
                    sb.append("Proxy-Authorization: ").append(authHeader).append("\r\n");
                }
                sb.append("Proxy-Connection: Keep-Alive\r\n");
                sb.append("\r\n");
                pOut.write(sb.toString().getBytes("UTF-8"));
                pOut.flush();

                // Leer respuesta del proxy (primera línea + cabeceras hasta \r\n\r\n)
                String statusLine = readLine(pIn);
                if (statusLine == null || !statusLine.contains(" 200 ")) {
                    L.w(TAG, "Proxy rechazó CONNECT: " + statusLine);
                    sendSocksReply(out, 0x05); // connection refused
                    proxy.close();
                    return;
                }
                // Consumir el resto de cabeceras
                String line;
                while ((line = readLine(pIn)) != null && !line.isEmpty()) {
                    // ignorar cabeceras
                }

                // --- SOCKS5 éxito ---
                sendSocksReply(out, 0x00);

                // --- Tubería bidireccional ---
                pipe(in, pOut, client, proxy);
                pipe(pIn, out, proxy, client);

            } catch (IOException e) {
                L.w(TAG, "Error conectando al proxy: " + e.getMessage());
                try { sendSocksReply(out, 0x05); } catch (IOException ignored) {}
                proxy.close();
            }

        } catch (IOException e) {
            L.w(TAG, "Error atendiendo cliente", e);
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    /** Respuesta SOCKS5: [VER=5][REP][RSV=0][ATYP=1][ADDR=0.0.0.0][PORT=0] */
    private void sendSocksReply(OutputStream out, int rep) throws IOException {
        out.write(new byte[]{
                0x05, (byte) rep, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00
        });
        out.flush();
    }

    /** Lee exactamente buf.length bytes o devuelve false si EOF. */
    private boolean readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) return false;
            off += n;
        }
        return true;
    }

    /** Lee una línea hasta \n (sin incluir el \n). Devuelve null si EOF. */
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

    /** Copia bytes de src a dst hasta EOF. Cierra ambos sockets al terminar. */
    private void pipe(InputStream src, OutputStream dst, Socket s1, Socket s2) {
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            int n;
            while ((n = src.read(buf)) != -1) {
                dst.write(buf, 0, n);
                dst.flush();
            }
        } catch (IOException e) {
            // Normal cuando una de las partes cierra la conexión.
        } finally {
            try { s1.close(); } catch (IOException ignored) {}
            try { s2.close(); } catch (IOException ignored) {}
        }
    }
}
