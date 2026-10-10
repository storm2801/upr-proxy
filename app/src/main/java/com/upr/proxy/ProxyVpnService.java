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

    public static final String ACTION_STATE_CHANGED = "com.upr.proxy.STATE_CHANGED";
    public static final String ACTION_STOP = "com.upr.proxy.STOP";

    private ParcelFileDescriptor vpnInterface;
    private Thread vpnThread;
    private ProxyForwarder forwarder;
    private String proxyServer = "proxy.upr.edu.cu";
    private String proxyPort = "8080";
    private String proxyUser = "";
    private String proxyPassword = "";
    private volatile boolean stopping = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // ¿Es una orden de STOP?
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            L.i(TAG, "STOP solicitado por el usuario");
            detenerTodo();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            String s = intent.getStringExtra("server");
            String p = intent.getStringExtra("port");
            String u = intent.getStringExtra("user");
            String pw = intent.getStringExtra("password");
            if (s != null) proxyServer = s;
            if (p != null) proxyPort = p;
            if (u != null) proxyUser = u;
            if (pw != null) proxyPassword = pw;
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        startVpn();
        return START_NOT_STICKY; // No reiniciar si el sistema mata el servicio
    }

    private void startVpn() {
        if (vpnInterface != null || stopping) return;
        try {
            Builder builder = new Builder();
            builder.setSession("UPR Proxy");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);
            // Excluir nuestra propia app del túnel para evitar bucles
            try { builder.addDisallowedApplication(getPackageName()); } catch (Exception ignored) {}

            vpnInterface = builder.establish();
            if (vpnInterface == null) {
                L.e(TAG, "No se pudo establecer la interfaz VPN");
                detenerTodo();
                return;
            }

            forwarder = new ProxyForwarder(getApplicationContext(), vpnInterface,
                    proxyServer, proxyPort, proxyUser, proxyPassword);
            vpnThread = new Thread(forwarder, "upr-proxy-forwarder");
            vpnThread.start();

            ProxyStatus.vpnRunning.set(true);
            ProxyStatus.serverHost = proxyServer;
            try { ProxyStatus.serverPort = Integer.parseInt(proxyPort); } catch (Exception ignored) {}
            ProxyStatus.startedAt = System.currentTimeMillis();

            broadcastState();
            L.i(TAG, "VPN activa → " + proxyServer + ":" + proxyPort);

        } catch (Exception e) {
            L.e(TAG, "Error al iniciar VPN", e);
            detenerTodo();
        }
    }

    /**
     * Detención ordenada y SIN BLOQUEOS.
     * El orden importa: primero cerramos el TUN (rompe cualquier
     * read() bloqueado del motor nativo), luego marcamos el estado,
     * luego lanzamos el shutdown del forwarder en un hilo aparte.
     */
    private void detenerTodo() {
        if (stopping) return;
        stopping = true;

        L.i(TAG, "Iniciando detención ordenada…");

        // 1. Marcar estado PRIMERO (la UI debe reflejarlo de inmediato)
        ProxyStatus.vpnRunning.set(false);
        broadcastState();

        // 2. Cerrar la interfaz VPN. Esto rompe el read() del motor nativo.
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) {}
            vpnInterface = null;
        }

        // 3. Interrumpir el hilo del forwarder
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }

        // 4. Shutdown del motor nativo + puente, en hilo aparte con timeout
        ProxyForwarder f = forwarder;
        forwarder = null;
        if (f != null) {
            new Thread(f::shutdown, "svc-forwarder-shutdown").start();
        }

        // 5. Quitar notificación y parar el servicio
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Exception ignored) {}

        stopSelf();
    }

    private void broadcastState() {
        Intent i = new Intent(ACTION_STATE_CHANGED);
        i.setPackage(getPackageName());
        sendBroadcast(i);
    }

    private Notification buildNotification() {
        Intent ni = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Intent disc = new Intent(this, ProxyVpnService.class);
        disc.setAction(ACTION_STOP);
        PendingIntent piDisc = PendingIntent.getService(this, 1, disc,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("UPR Proxy activo")
                .setContentText("Redirigiendo a " + proxyServer + ":" + proxyPort)
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pi)
                .addAction(0, "Desconectar", piDisc)
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
        L.w(TAG, "VPN revocada por el sistema");
        detenerTodo();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        L.i(TAG, "onDestroy — asegurando detención");
        if (!stopping) {
            detenerTodo();
        }
        super.onDestroy();
    }
}
