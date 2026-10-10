package com.upr.proxy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProxyStatus {

    public static final AtomicBoolean vpnRunning = new AtomicBoolean(false);
    public static volatile String serverHost = "";
    public static volatile int serverPort = 0;
    public static final AtomicInteger activeConnections = new AtomicInteger(0);
    public static final AtomicLong bytesSent = new AtomicLong(0);
    public static final AtomicLong totalConnections = new AtomicLong(0);
    public static volatile long startedAt = 0;

    // Cálculo de velocidad
    private static final AtomicLong lastBytes = new AtomicLong(0);
    private static final AtomicLong lastCheck = new AtomicLong(0);
    public static volatile long speedBps = 0;

    public static void reset() {
        activeConnections.set(0);
        bytesSent.set(0);
        totalConnections.set(0);
        startedAt = 0;
        lastBytes.set(0);
        lastCheck.set(0);
        speedBps = 0;
    }

    /** Llamar periódicamente (1 vez por segundo) para calcular la velocidad. */
    public static void updateSpeed() {
        long now = System.currentTimeMillis();
        long last = lastCheck.get();
        if (last == 0) {
            lastCheck.set(now);
            lastBytes.set(bytesSent.get());
            return;
        }
        long dt = now - last;
        if (dt >= 800) {
            long currentBytes = bytesSent.get();
            long dBytes = currentBytes - lastBytes.get();
            if (dBytes < 0) dBytes = 0;
            speedBps = (dBytes * 1000) / dt;
            lastBytes.set(currentBytes);
            lastCheck.set(now);
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public static String formatSpeed() {
        long s = speedBps;
        if (s < 1024) return s + " B/s";
        if (s < 1024 * 1024) return String.format("%.1f KB/s", s / 1024.0);
        return String.format("%.1f MB/s", s / (1024.0 * 1024));
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
