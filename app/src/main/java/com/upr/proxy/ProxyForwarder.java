package com.upr.proxy;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import org.amnezia.awg.hevtunnel.TProxyService;

import java.io.File;
import java.io.FileWriter;

/**
 * Orquestador del túnel:
 *   1. Arranca Socks5HttpBridge en 127.0.0.1:1080
 *   2. Escribe la config YAML de hev-socks5-tunnel apuntando a ese puente
 *   3. Llama a la librería nativa TProxyStartService con el fd del TUN
 *
 * Al detenerse, invierte el orden: primero el túnel nativo, después el puente.
 */
public class ProxyForwarder implements Runnable {

    private static final String TAG = "ProxyForwarder";

    private final Context context;
    private final ParcelFileDescriptor vpnInterface;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUser;
    private final String proxyPassword;

    private Socks5HttpBridge bridge;

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
            // ---- 1. Arrancar el puente SOCKS5→HTTP ----
            bridge = new Socks5HttpBridge(proxyHost, proxyPort, proxyUser, proxyPassword);
            bridge.start();
            L.i(TAG, "Puente SOCKS5→HTTP iniciado en 127.0.0.1:1080");

            // ---- 2. Escribir config del túnel ----
            File cfg = new File(context.getCacheDir(), "hev.yaml");
            try (FileWriter w = new FileWriter(cfg)) {
                w.write("tunnel:\n");
                w.write("  mtu: 8500\n");
                w.write("\n");
                w.write("socks5:\n");
                w.write("  port: 1080\n");
                w.write("  address: 127.0.0.1\n");
                w.write("  udp: 'udp'\n");
                w.write("\n");
                w.write("misc:\n");
                w.write("  task-stack-size: 20480\n");
                w.write("  connect-timeout: 10000\n");
                w.write("  read-write-timeout: 60000\n");
                w.write("  log-file: stderr\n");
                w.write("  log-level: warn\n");
                w.write("  limit-nofile: 65535\n");
            }

            // ---- 3. Arrancar el motor nativo ----
            int tunFd = vpnInterface.getFd();
            L.i(TAG, "Llamando TProxyStartService(fd=" + tunFd + ")");
            TProxyService.TProxyStartService(cfg.getAbsolutePath(), tunFd);
            L.i(TAG, "TProxyStartService OK — el tráfico debería fluir");

        } catch (Throwable t) {
            L.e(TAG, "Error arrancando el túnel", t);
        }
    }

    /** Llamar al detener el servicio VPN. */
    public void shutdown() {
        try {
            TProxyService.TProxyStopService();
            L.i(TAG, "TProxyStopService OK");
        } catch (Throwable t) {
            L.w(TAG, "TProxyStopService falló: " + t.getMessage());
        }
        if (bridge != null) {
            bridge.stop();
            bridge = null;
        }
    }
}
