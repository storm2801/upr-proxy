package com.upr.proxy;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.amnezia.awg.hevtunnel.TProxyService;

import java.io.File;
import java.io.FileWriter;

/**
 * PASO 1: escribe una config YAML y llama al servicio nativo del AAR.
 * La config apunta a un SOCKS5 en 127.0.0.1:1080 (que aún no existe).
 * Solo buscamos confirmar que el AAR se carga y el método nativo arranca.
 */
public class ProxyForwarder implements Runnable {

    private static final String TAG = "ProxyForwarder";
    private final Context context;
    private final ParcelFileDescriptor vpnInterface;
    private final String proxyHost;
    private final int proxyPort;

    public ProxyForwarder(Context ctx, ParcelFileDescriptor vpn,
                          String host, String port,
                          String user, String pass) {
        this.context = ctx;
        this.vpnInterface = vpn;
        this.proxyHost = host;
        int p = 8080;
        try { p = Integer.parseInt(port); } catch (Exception ignored) {}
        this.proxyPort = p;
    }

    @Override
    public void run() {
        try {
            File cfg = new File(context.getCacheDir(), "hev.yaml");
            try (FileWriter w = new FileWriter(cfg)) {
                w.write("tunnel:\n");
                w.write("  mtu: 8500\n");
                w.write("\n");
                w.write("socks5:\n");
                w.write("  port: 1080\n");
                w.write("  address: 127.0.0.1\n");
                w.write("\n");
                w.write("misc:\n");
                w.write("  task-stack-size: 20480\n");
                w.write("  connect-timeout: 5000\n");
                w.write("  read-write-timeout: 60000\n");
                w.write("  log-file: stderr\n");
                w.write("  log-level: debug\n");
                w.write("  limit-nofile: 65535\n");
            }
            Log.i(TAG, "Config escrita en: " + cfg.getAbsolutePath());

            try {
                System.loadLibrary("hev-socks5-tunnel");
                Log.i(TAG, "librería cargada explícitamente OK");
            } catch (UnsatisfiedLinkError e) {
                Log.w(TAG, "loadLibrary falló (probablemente ya cargada por AAR): " + e.getMessage());
            }

            int tunFd = vpnInterface.getFd();
            Log.i(TAG, "Llamando TProxyStartService(" + cfg.getAbsolutePath() + ", fd=" + tunFd + ")");
            TProxyService.TProxyStartService(cfg.getAbsolutePath(), tunFd);
            Log.i(TAG, "TProxyStartService regresó OK");

        } catch (Throwable t) {
            Log.e(TAG, "Error en ProxyForwarder", t);
        }
    }
}
