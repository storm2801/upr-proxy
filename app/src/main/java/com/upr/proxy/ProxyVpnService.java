package com.upr.proxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class ProxyVpnService extends VpnService {

    private static final String TAG = "ProxyVpnService";
    private static final String CHANNEL_ID = "upr_proxy_channel";
    private static final int NOTIFICATION_ID = 1;

    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;
    private ProxyForwarder forwarder;
    private String proxyServer = "proxy.upr.edu.cu";
    private String proxyPort = "8080";
    private String proxyUser = "";
    private String proxyPassword = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            proxyServer = intent.getStringExtra("server");
            proxyPort = intent.getStringExtra("port");
            proxyUser = intent.getStringExtra("user");
            proxyPassword = intent.getStringExtra("password");
        }
        if (proxyServer == null) proxyServer = "proxy.upr.edu.cu";
        if (proxyPort == null) proxyPort = "8080";
        if (proxyUser == null) proxyUser = "";
        if (proxyPassword == null) proxyPassword = "";

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        if (vpnInterface != null) return;
        try {
            Builder builder = new Builder();
            builder.setSession("UPR Proxy");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);

            // Añadir apps excluidas si hace falta (por ejemplo para no capturar
            // el propio tráfico del proxy). Aquí lo dejamos tal cual.
            // builder.addDisallowedApplication(getPackageName());

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                L.e(TAG, "No se pudo establecer la interfaz VPN");
                stopSelf();
                return;
            }

            forwarder = new ProxyForwarder(getApplicationContext(), vpnInterface,
                    proxyServer, proxyPort, proxyUser, proxyPassword);
            vpnThread = new Thread(forwarder, "upr-proxy-forwarder");
            vpnThread.start();

            L.i(TAG, "VPN activa → proxy " + proxyServer + ":" + proxyPort);

        } catch (Exception e) {
            L.e(TAG, "Error al iniciar VPN", e);
            stopSelf();
        }
    }

    private Notification buildNotification() {
        Intent ni = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UPR Proxy activo")
                .setContentText("Redirigiendo a " + proxyServer + ":" + proxyPort)
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "UPR Proxy", NotificationManager.IMPORTANCE_LOW);
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(ch);
        }
    }

    @Override
    public void onRevoke() {
        // El sistema revocó el permiso VPN (por ejemplo, otra VPN tomó el control)
        L.w(TAG, "VPN revocada por el sistema");
        shutdownEverything();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        shutdownEverything();
        super.onDestroy();
    }

    private void shutdownEverything() {
        // Orden: primero el motor nativo, luego el puente Java, luego el fd
        if (forwarder != null) {
            forwarder.shutdown();
            forwarder = null;
        }
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) {}
            vpnInterface = null;
        }
    }
}
