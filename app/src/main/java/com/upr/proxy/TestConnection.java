package com.upr.proxy;

import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hace una prueba de CONNECT contra el proxy HTTP sin activar la VPN.
 * Sirve para diagnosticar credenciales / accesibilidad antes de conectar.
 */
public class TestConnection {

    private static final String TAG = "TestConnection";
    private static final int TIMEOUT_MS = 8000;
    private static final String HOST_TEST = "www.google.com";
    private static final int PORT_TEST = 443;

    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Handler ui = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean running = new AtomicBoolean(false);

    public interface Callback {
        void onResult(boolean ok, String message);
    }

    public static void run(String host, int port, String user, String pass, Callback cb) {
        if (running.getAndSet(true)) {
            cb.onResult(false, "Ya hay una prueba en curso");
            return;
        }

        pool.submit(() -> {
            String result;
            boolean ok = false;
            try {
                ok = test(host, port, user, pass);
                if (ok) {
                    result = "✅ El proxy responde y acepta tus credenciales";
                } else {
                    result = "❌ El proxy respondió pero rechazó la petición";
                }
            } catch (ProxyAuthException e) {
                result = "❌ Usuario o contraseña incorrectos";
            } catch (java.net.SocketTimeoutException e) {
                result = "❌ Timeout: el proxy no responde (¿estás en la red de la UPR?)";
            } catch (java.net.UnknownHostException e) {
                result = "❌ No se pudo resolver " + host + " (¿estás en la red de la UPR?)";
            } catch (java.net.ConnectException e) {
                result = "❌ No se pudo conectar a " + host + ":" + port;
            } catch (Exception e) {
                result = "❌ Error: " + e.getClass().getSimpleName() + " — " + e.getMessage();
            } finally {
                running.set(false);
            }

            final boolean fOk = ok;
            final String fRes = result;
            ui.post(() -> cb.onResult(fOk, fRes));
        });
    }

    private static boolean test(String host, int port, String user, String pass) throws Exception {
        // Si el host es el dominio, probar primero el dominio y luego la IP directa
        if ("proxy.upr.edu.cu".equals(host)) {
            try {
                return testDirecto("proxy.upr.edu.cu", port, user, pass);
            } catch (java.net.UnknownHostException e) {
                L.i(TAG, "Dominio no resuelve, probando IP directa 10.2.1.80");
                return testDirecto("10.2.1.80", port, user, pass);
            } catch (Exception e) {
                L.i(TAG, "Dominio falló (" + e.getMessage() + "), probando IP directa");
                return testDirecto("10.2.1.80", port, user, pass);
            }
        }
        return testDirecto(host, port, user, pass);
    }

    private static boolean testDirecto(String host, int port, String user, String pass) throws Exception {
        L.i(TAG, "Probando contra " + host + ":" + port);

        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            s.setSoTimeout(TIMEOUT_MS);

            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            StringBuilder sb = new StringBuilder();
            sb.append("CONNECT ").append(HOST_TEST).append(":").append(PORT_TEST)
                    .append(" HTTP/1.1\r\n");
            sb.append("Host: ").append(HOST_TEST).append(":").append(PORT_TEST).append("\r\n");
            if (user != null && !user.isEmpty()) {
                String cred = user + ":" + (pass == null ? "" : pass);
                String auth = "Basic " + android.util.Base64.encodeToString(
                        cred.getBytes(), android.util.Base64.NO_WRAP);
                sb.append("Proxy-Authorization: ").append(auth).append("\r\n");
            }
            sb.append("Proxy-Connection: Keep-Alive\r\n");
            sb.append("\r\n");
            out.write(sb.toString().getBytes("UTF-8"));
            out.flush();

            String statusLine = readLine(in);
            L.i(TAG, "Respuesta del proxy: " + statusLine);

            if (statusLine == null) return false;

            if (statusLine.contains(" 200 ")) return true;
            if (statusLine.contains(" 407 ")) throw new ProxyAuthException();

            // Consumir cabeceras para no dejar colgado el socket
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) { /* ignorar */ }

            return false;
        } finally {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    private static String readLine(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') sb.append((char) c);
            if (sb.length() > 4096) break;
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    static class ProxyAuthException extends Exception {
        ProxyAuthException() { super("Credenciales rechazadas"); }
    }
}
