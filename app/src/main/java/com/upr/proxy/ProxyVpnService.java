package com.upr.proxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

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
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Runnable notifUpdater;

    /** IPs que NO deben pasar por el túnel VPN (evita loops con el proxy local). */
    private static final String[] EXCLUDED_ROUTES = {
            "10.2.1.80",      // proxy de la UPR (fallback directo)
            "10.2.144.1",     // DNS del sistema de la UPR
            "10.2.1.8",       // DNS alternativo de la UPR
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        // Aumentar prioridad para Samsung/Android 12+
        Notification notif = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notif);
        }

        startVpn();
        iniciarActualizadorNotif();
        return START_STICKY; // Intentar que el sistema lo reviva
    }

    private void startVpn() {
        if (vpnInterface != null) return;
        stopping = false;

        try {
            Builder builder = new Builder();
            builder.setSession("UPR Proxy");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);

            // NOTA: se omite intencionalmente IPv6. El proxy corporativo (squid)
            // devolvía 503 Service Unavailable para CONNECT a direcciones IPv6,
            // lo que rompía el tráfico de apps como Facebook/WhatsApp que usan
            // AAAA. Al solo dar IPv4 en el túnel, el kernel fuerza IPv4 y las
            // apps caen al registro A (happy eyeballs).

            // Excluir del túnel las IPs del proxy y DNS locales:
            // Usamos rutas con prefijos específicos para garantizar que el
            // tráfico al proxy de la UPR NO pase por la interfaz VPN,
            // evitando el bucle de enrutamiento.
            for (String ip : EXCLUDED_ROUTES) {
                try {
                    // Usamos /32 para rutas específicas de host
                    builder.addRoute(ip, 32);
                    L.i(TAG, "Ruta específica excluida: " + ip + "/32");
                } catch (Exception e) {
                    L.w(TAG, "No se pudo excluir ruta " + ip + ": " + e.getMessage());
                }
            }

            // Configurar DNS del sistema de la UPR dentro del túnel.
            // Con addRoute("0.0.0.0", 0) el móvil solo usa los DNS que declara
            // el Builder; sin ellos las consultas DNS se pierden y los dominios
            // no se resuelven → timeout al conectar.
            // Se usan los DNS reales de la UPR (no el mapdns ficticio 198.18.0.2,
            // que no resolvía nada y causaba fallos de conexión).
            for (String dns : new String[]{"10.2.144.1", "10.2.1.8"}) {
                try {
                    builder.addDnsServer(InetAddress.getByName(dns));
                    L.i(TAG, "DNS del túnel: " + dns);
                } catch (Exception e) {
                    L.w(TAG, "No se pudo añadir DNS " + dns + ": " + e.getMessage());
                }
            }

            try { builder.addDisallowedApplication(getPackageName()); } catch (Exception ignored) {}

            vpnInterface = builder.establish();

            if (vpnInterface == null) {
                L.e(TAG, "establish() devolvió null — la VPN no pudo crearse");
                detenerTodo();
                return;
            }

            L.i(TAG, "✅ VPN establecida");

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

        } catch (Throwable t) {
            L.e(TAG, "Error al iniciar VPN: " + t.getMessage());
            detenerTodo();
        }
    }

    private void iniciarActualizadorNotif() {
        notifUpdater = new Runnable() {
            @Override public void run() {
                if (stopping) return;
                ProxyStatus.updateSpeed();
                try {
                    NotificationManager nm =
                            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
                } catch (Exception ignored) {}
                ui.postDelayed(this, 2000);
            }
        };
        ui.postDelayed(notifUpdater, 2000);
    }

    private void detenerTodo() {
        if (stopping) return;
        stopping = true;

        if (notifUpdater != null) {
            ui.removeCallbacks(notifUpdater);
            notifUpdater = null;
        }

        L.i(TAG, "Iniciando detención ordenada…");
        ProxyStatus.vpnRunning.set(false);
        broadcastState();

        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) {}
            vpnInterface = null;
        }
        if (vpnThread != null) {
            vpnThread.interrupt();
            vpnThread = null;
        }

        ProxyForwarder f = forwarder;
        forwarder = null;
        if (f != null) {
            new Thread(f::shutdown, "svc-forwarder-shutdown").start();
        }

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

        String title = "UPR Proxy activo";
        String sub = "↑ " + ProxyStatus.formatSpeedUp()
                + "  ↓ " + ProxyStatus.formatSpeedDown()
                + "  •  " + ProxyStatus.activeConnections.get() + " conexiones";

        String bigText =
                "Servidor: " + proxyServer + ":" + proxyPort
                + "\n\n"
                + "↑  Subida: " + ProxyStatus.formatSpeedUp()
                + "\n"
                + "↓  Bajada: " + ProxyStatus.formatSpeedDown()
                + "\n\n"
                + "Conexiones: " + ProxyStatus.activeConnections.get()
                + "\n"
                + "Datos totales: " + ProxyStatus.formatBytes(
                        ProxyStatus.bytesSent.get() + ProxyStatus.bytesReceived.get())
                + "\n"
                + "Tiempo: " + ProxyStatus.formatUptime(ProxyStatus.startedAt);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(sub)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFF107530)
                .setContentIntent(pi)
                .addAction(0, "Desconectar", piDisc)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "UPR Proxy", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            ch.enableLights(false);
            ch.enableVibration(false);
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
        if (!stopping) detenerTodo();
        super.onDestroy();
    }
}
