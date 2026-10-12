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
        startForeground(NOTIFICATION_ID, buildNotification());
        startVpn();
        iniciarActualizadorNotif();
        return START_NOT_STICKY;
    }

    private void startVpn() {
        if (vpnInterface != null || stopping) return;

        L.i(TAG, "Iniciando VPN con detección de compatibilidad…");

        // Leer DNS del sistema una sola vez
        List<InetAddress> dnsList = new ArrayList<>();
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network active = cm.getActiveNetwork();
                LinkProperties lp = cm.getLinkProperties(active);
                if (lp != null) {
                    dnsList.addAll(lp.getDnsServers());
                    L.i(TAG, "DNS del sistema: " + dnsList);
                }
            }
        } catch (Exception e) {
            L.w(TAG, "No se pudo leer DNS del sistema: " + e.getMessage());
        }

        // Filtrar solo IPv4 (la VPN solo declara IPv4)
        List<InetAddress> ipv4Dns = new ArrayList<>();
        for (InetAddress dns : dnsList) {
            if (dns instanceof java.net.Inet4Address) {
                ipv4Dns.add(dns);
            } else {
                L.i(TAG, "DNS IPv6 ignorado: " + dns);
            }
        }

        // Fallbacks si no hay IPv4
        if (ipv4Dns.isEmpty()) {
            try {
                ipv4Dns.add(InetAddress.getByName("10.2.0.1"));
                ipv4Dns.add(InetAddress.getByName("8.8.8.8"));
                L.i(TAG, "DNS fallback IPv4: " + ipv4Dns);
            } catch (Exception ignored) {}
        }

        // Sistema de intentos en cascada para máxima compatibilidad
        vpnInterface = tryEstablishStrategyA(ipv4Dns);
        if (vpnInterface == null) {
            L.w(TAG, "Estrategia A (excludeRoute) falló, probando B…");
            vpnInterface = tryEstablishStrategyB(ipv4Dns);
        }
        if (vpnInterface == null) {
            L.w(TAG, "Estrategia B (addDnsServer) falló, probando C…");
            vpnInterface = tryEstablishStrategyC();
        }

        if (vpnInterface == null) {
            L.e(TAG, "Todas las estrategias fallaron. La VPN no puede establecerse.");
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
    }

    /**
     * Estrategia A: DNS + excludeRoute (óptima en stock Android, Pixel).
     * Excluye las consultas DNS del túnel para que vayan directas por WiFi.
     */
    private ParcelFileDescriptor tryEstablishStrategyA(List<InetAddress> dnsList) {
        try {
            Builder builder = new Builder();
            builder.setSession("UPR Proxy");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);

            for (InetAddress dns : dnsList) {
                builder.addDnsServer(dns);
                try {
                    builder.excludeRoute(new android.net.IpPrefix(dns, 32));
                } catch (Exception e) {
                    L.w(TAG, "[A] excludeRoute(" + dns + ") falló: " + e.getMessage());
                }
            }

            try { builder.addDisallowedApplication(getPackageName()); } catch (Exception ignored) {}

            ParcelFileDescriptor pfd = builder.establish();
            if (pfd != null) {
                L.i(TAG, "[A] ✅ VPN establecida con excludeRoute");
            }
            return pfd;
        } catch (Exception e) {
            L.w(TAG, "[A] Exception: " + e.getMessage());
            return null;
        }
    }

    /**
     * Estrategia B: DNS sin excludeRoute (compatible con Samsung, Xiaomi).
     * DNS va por el túnel pero el usuario puede al menos conectarse.
     */
    private ParcelFileDescriptor tryEstablishStrategyB(List<InetAddress> dnsList) {
        try {
            Builder builder = new Builder();
            builder.setSession("UPR Proxy");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);

            for (InetAddress dns : dnsList) {
                builder.addDnsServer(dns);
            }

            try { builder.addDisallowedApplication(getPackageName()); } catch (Exception ignored) {}

            ParcelFileDescriptor pfd = builder.establish();
            if (pfd != null) {
                L.i(TAG, "[B] ✅ VPN establecida sin excludeRoute");
            }
            return pfd;
        } catch (Exception e) {
            L.w(TAG, "[B] Exception: " + e.getMessage());
            return null;
        }
    }

    /**
     * Estrategia C: VPN básica sin DNS ni rutas especiales (último recurso).
     * Compatible con cualquier Android, aunque el DNS puede no resolverse.
     */
    private ParcelFileDescriptor tryEstablishStrategyC() {
        try {
            Builder builder = new Builder();
            builder.setSession("UPR Proxy");
            builder.addAddress("10.0.0.2", 32);
            builder.addRoute("0.0.0.0", 0);
            try { builder.addDisallowedApplication(getPackageName()); } catch (Exception ignored) {}

            ParcelFileDescriptor pfd = builder.establish();
            if (pfd != null) {
                L.i(TAG, "[C] ✅ VPN establecida en modo básico");
            }
            return pfd;
        } catch (Exception e) {
            L.w(TAG, "[C] Exception: " + e.getMessage());
            return null;
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
