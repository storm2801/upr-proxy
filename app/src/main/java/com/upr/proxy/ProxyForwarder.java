package com.upr.proxy;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import org.amnezia.awg.hevtunnel.TProxyService;

import java.io.File;
import java.io.FileWriter;

/**
 * Orquestador del túnel. Arranca el puente SOCKS5→HTTP, escribe la
 * config YAML y llama a la librería nativa. El shutdown se ejecuta
 * en un hilo aparte con timeout para que NUNCA bloquee al servicio.
 */
public class ProxyForwarder implements Runnable {

    private static final String TAG = "ProxyForwarder";
    private static final int SHUTDOWN_TIMEOUT_MS = 3000;

    private final Context context;
    private final ParcelFileDescriptor vpnInterface;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUser;
    private final String proxyPassword;

    private volatile Socks5HttpBridge bridge;

    public ProxyForwarder(Context ctx, ParcelFileDescriptor vpn,
                          String host, String port,
                          String user, String pass) {
        this.context = ctx;
        this.vpnInterface = vpn;
        this.proxyHost = host;
        int p = 8080;
        try { p = Integer.parseInt(port); } catch (Exception ignored) {}
        this.proxyPort = p;
        this.proxyUser = user;
        this.proxyPassword = pass;
    }

    @Override
    public void run() {
        try {
            bridge = new Socks5HttpBridge(proxyHost, proxyPort, proxyUser, proxyPassword);
            bridge.start();
            L.i(TAG, "Puente SOCKS5→HTTP iniciado en 127.0.0.1:1080");

            File cfg = new File(context.getCacheDir(), "hev.yaml");
            try (FileWriter w = new FileWriter(cfg)) {
                w.write("tunnel:\n  mtu: 8500\n\n");
                w.write("socks5:\n  port: 1080\n  address: 127.0.0.1\n\n");
                w.write("misc:\n");
                w.write("  task-stack-size: 20480\n");
                w.write("  connect-timeout: 10000\n");
                w.write("  read-write-timeout: 60000\n");
                w.write("  log-file: stderr\n");
                w.write("  log-level: warn\n");
                w.write("  limit-nofile: 65535\n");
            }

            int tunFd = vpnInterface.getFd();
            L.i(TAG, "Llamando TProxyStartService(fd=" + tunFd + ")");
            TProxyService.TProxyStartService(cfg.getAbsolutePath(), tunFd);
            L.i(TAG, "TProxyStartService OK");

        } catch (Throwable t) {
            L.e(TAG, "Error arrancando el túnel", t);
        }
    }

    /**
     * Llamar al detener el servicio VPN.
     * Se ejecuta en un hilo aparte con timeout para no bloquear nunca.
     */
    public void shutdown() {
        Thread t = new Thread(() -> {
            try {
                L.i(TAG, "Deteniendo motor nativo…");
                TProxyService.TProxyStopService();
                L.i(TAG, "TProxyStopService OK");
            } catch (Throwable e) {
                L.w(TAG, "TProxyStopService falló: " + e.getMessage());
            }
            Socks5HttpBridge b = bridge;
            if (b != null) {
                b.stop();
                bridge = null;
            }
        }, "forwarder-shutdown");
        t.setDaemon(true);
        t.start();

        try {
            t.join(SHUTDOWN_TIMEOUT_MS);
            if (t.isAlive()) {
                L.w(TAG, "Shutdown tardó más de " + SHUTDOWN_TIMEOUT_MS
                        + " ms, continuando sin esperar");
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
