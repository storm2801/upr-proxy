package com.upr.proxy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estado global del proxy en tiempo real.
 * Lo escribe ProxyVpnService / Socks5HttpBridge / TestConnection,
 * y lo lee MainActivity para pintar la UI.
 */
public class ProxyStatus {

    // VPN corriendo (arrancada por el usuario y no detenida)
    public static final AtomicBoolean vpnRunning = new AtomicBoolean(false);

    // Datos del servidor
    public static volatile String serverHost = "";
    public static volatile int serverPort = 0;

    // Conexiones actualmente abiertas
    public static final AtomicInteger activeConnections = new AtomicInteger(0);

    // Total de bytes enviados al proxy
    public static final AtomicLong bytesSent = new AtomicLong(0);

    // Total de conexiones desde el arranque
    public static final AtomicLong totalConnections = new AtomicLong(0);

    // Momento de arranque (ms)
    public static volatile long startedAt = 0;

    public static void reset() {
        activeConnections.set(0);
        bytesSent.set(0);
        totalConnections.set(0);
        startedAt = 0;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String formatUptime(long startedAtMs) {
        if (startedAtMs == 0) return "—";
        long secs = (System.currentTimeMillis() - startedAtMs) / 1000;
        if (secs < 60) return secs + " s";
        long mins = secs / 60;
        long rem = secs % 60;
        if (mins < 60) return mins + " min " + rem + " s";
        long hours = mins / 60;
        return hours + " h " + (mins % 60) + " min";
    }
}
