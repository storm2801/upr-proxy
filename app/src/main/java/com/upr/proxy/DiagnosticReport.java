package com.upr.proxy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Genera un reporte de diagnóstico completo con toda la información
 * necesaria para depurar problemas de la app.
 */
public class DiagnosticReport {

    private static final String TAG = "DiagnosticReport";

    public static void generateAndShare(AppCompatActivity ctx) {
        try {
            String report = buildReport(ctx);

            File dir = new File(ctx.getCacheDir(), "diagnostics");
            if (!dir.exists()) dir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                    .format(new Date());
            File out = new File(dir, "upr-diagnostico-" + timestamp + ".txt");

            try (FileWriter w = new FileWriter(out)) {
                w.write(report);
            }

            L.i(TAG, "Reporte generado: " + out.getAbsolutePath()
                    + " (" + out.length() + " bytes)");

            Uri uri = FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".fileprovider", out);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico UPR Proxy");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(share, "Compartir reporte"));

        } catch (Exception e) {
            L.e(TAG, "Error generando reporte", e);
        }
    }

    private static String buildReport(Context ctx) {
        StringBuilder sb = new StringBuilder();
        String line = "═══════════════════════════════════════════════════════\n";

        // Cabecera
        sb.append(line);
        sb.append("  UPR PROXY — REPORTE DE DIAGNÓSTICO\n");
        sb.append("  ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.US).format(new Date())).append("\n");
        sb.append(line).append("\n");

        // 1. Dispositivo
        sb.append("═══ 1. INFORMACIÓN DEL DISPOSITIVO ═══\n");
        sb.append("Fabricante:        ").append(Build.MANUFACTURER).append("\n");
        sb.append("Marca:             ").append(Build.BRAND).append("\n");
        sb.append("Modelo:            ").append(Build.MODEL).append("\n");
        sb.append("Producto:          ").append(Build.PRODUCT).append("\n");
        sb.append("Hardware:          ").append(Build.HARDWARE).append("\n");
        sb.append("Android:           ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        if (Build.VERSION.SDK_INT >= 23) {
            sb.append("Security patch:    ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        }
        sb.append("Arquitecturas:     ");
        for (int i = 0; i < Build.SUPPORTED_ABIS.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(Build.SUPPORTED_ABIS[i]);
        }
        sb.append("\n");
        sb.append("Idioma:            ").append(Locale.getDefault()).append("\n");
        sb.append("Zona horaria:      ").append(TimeZone.getDefault().getID()).append("\n");
        sb.append("Rooted:            ").append(isRooted() ? "Sí" : "No").append("\n");
        sb.append("Emulador:          ").append(isEmulator() ? "Sí" : "No").append("\n\n");

        // 2. Memoria y almacenamiento
        sb.append("═══ 2. MEMORIA Y ALMACENAMIENTO ═══\n");
        try {
            ActivityManager am = (ActivityManager)
                    ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                sb.append(String.format(Locale.US, "RAM total:         %.2f GB\n",
                        mi.totalMem / (1024.0 * 1024 * 1024)));
                sb.append(String.format(Locale.US, "RAM disponible:    %.2f GB\n",
                        mi.availMem / (1024.0 * 1024 * 1024)));
                sb.append("Low memory:        ").append(mi.lowMemory ? "Sí" : "No").append("\n");
            }
        } catch (Exception ignored) {}

        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
            long blockSize = stat.getBlockSizeLong();
            long total = stat.getBlockCountLong() * blockSize;
            long avail = stat.getAvailableBlocksLong() * blockSize;
            sb.append(String.format(Locale.US, "Almacenamiento:    %.1f GB libres / %.1f GB totales\n",
                    avail / (1024.0 * 1024 * 1024),
                    total / (1024.0 * 1024 * 1024)));
        } catch (Exception ignored) {}
        sb.append("\n");

        // 3. Pantalla
        sb.append("═══ 3. PANTALLA ═══\n");
        try {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            sb.append("Resolución:        ").append(dm.widthPixels).append(" x ")
                    .append(dm.heightPixels).append(" px\n");
            sb.append(String.format(Locale.US, "Densidad:          %d dpi (%.2f x)\n",
                    dm.densityDpi, dm.density));
        } catch (Exception ignored) {}
        sb.append("\n");

        // 4. App
        sb.append("═══ 4. INFORMACIÓN DE LA APP ═══\n");
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
            sb.append("Package:           ").append(pi.packageName).append("\n");
            sb.append("Versión:           ").append(pi.versionName)
                    .append(" (").append(pi.versionCode).append(")\n");
            sb.append("Primera instalación: ")
                    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .format(new Date(pi.firstInstallTime))).append("\n");
            sb.append("Última actualización: ")
                    .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .format(new Date(pi.lastUpdateTime))).append("\n");

            ApplicationInfo ai = pi.applicationInfo;
            File apk = new File(ai.sourceDir);
            sb.append("Ruta del APK:      ").append(ai.sourceDir).append("\n");
            sb.append(String.format(Locale.US, "Tamaño del APK:    %.2f MB\n",
                    apk.length() / (1024.0 * 1024)));
            sb.append("Target SDK:        ").append(ai.targetSdkVersion).append("\n");
            sb.append("Debug:             ")
                    .append((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? "Sí" : "No")
                    .append("\n");

            try {
                Signature[] sigs = pm.getPackageInfo(ctx.getPackageName(),
                        PackageManager.GET_SIGNATURES).signatures;
                if (sigs != null && sigs.length > 0) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hash = md.digest(sigs[0].toByteArray());
                    StringBuilder hex = new StringBuilder();
                    for (byte b : hash) hex.append(String.format("%02x", b));
                    sb.append("Firma SHA-256:     ")
                            .append(hex.substring(0, Math.min(32, hex.length())))
                            .append("...\n");
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            sb.append("Error obteniendo info de la app: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n");

        // 5. Config
        sb.append("═══ 5. CONFIGURACIÓN GUARDADA ═══\n");
        try {
            SharedPreferences prefs = SecurePrefs.get(ctx);
            sb.append("Servidor:          ").append(prefs.getString("server", "—")).append("\n");
            sb.append("Puerto:            ").append(prefs.getString("port", "—")).append("\n");
            sb.append("Usuario:           ").append(prefs.getString("user", "—")).append("\n");
            String pass = prefs.getString("password", "");
            sb.append("Contraseña:        ")
                    .append(pass.isEmpty() ? "(vacía)" : "(oculta, " + pass.length() + " caracteres)")
                    .append("\n");
            sb.append("Modo dev:          ")
                    .append(prefs.getBoolean("developer_mode", false) ? "Sí" : "No").append("\n");
            sb.append("Onboarding:        ")
                    .append(prefs.getBoolean("onboarding_done", false) ? "Completado" : "Pendiente")
                    .append("\n");
        } catch (Exception e) {
            sb.append("Error: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n");

        // 6. VPN
        sb.append("═══ 6. ESTADO DE LA VPN ═══\n");
        sb.append("VPN activa:        ").append(ProxyStatus.vpnRunning.get() ? "Sí" : "No").append("\n");
        sb.append("Servidor proxy:    ").append(ProxyStatus.serverHost)
                .append(":").append(ProxyStatus.serverPort).append("\n");
        sb.append("Uptime:            ").append(ProxyStatus.formatUptime(ProxyStatus.startedAt)).append("\n");
        sb.append("Conexiones activas: ").append(ProxyStatus.activeConnections.get()).append("\n");
        sb.append("Conexiones totales: ").append(ProxyStatus.totalConnections.get()).append("\n");
        sb.append("Bytes enviados:    ").append(ProxyStatus.formatBytes(ProxyStatus.bytesSent.get())).append("\n");
        sb.append("Bytes recibidos:   ").append(ProxyStatus.formatBytes(ProxyStatus.bytesReceived.get())).append("\n");
        sb.append("Velocidad ↑:       ").append(ProxyStatus.formatSpeedUp()).append("\n");
        sb.append("Velocidad ↓:       ").append(ProxyStatus.formatSpeedDown()).append("\n\n");

        // 7. Red
        sb.append("═══ 7. ESTADO DE LA RED ═══\n");
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network active = cm.getActiveNetwork();
                NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                LinkProperties lp = cm.getLinkProperties(active);

                if (caps != null) {
                    String tipo = "Desconocido";
                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) tipo = "WiFi";
                    else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) tipo = "Móvil";
                    else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) tipo = "Ethernet";
                    else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) tipo = "VPN";
                    sb.append("Tipo de red:       ").append(tipo).append("\n");
                    sb.append("Internet:          ")
                            .append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ? "Sí" : "No")
                            .append("\n");
                    sb.append("Validated:         ")
                            .append(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ? "Sí" : "No")
                            .append("\n");
                }

                if (lp != null) {
                    if (lp.getLinkAddresses().size() > 0) {
                        sb.append("IP local:          ")
                                .append(lp.getLinkAddresses().get(0).getAddress().getHostAddress())
                                .append("\n");
                    }
                    if (lp.getRoutes().size() > 0 && lp.getRoutes().get(0).getGateway() != null) {
                        sb.append("Gateway:           ")
                                .append(lp.getRoutes().get(0).getGateway().getHostAddress())
                                .append("\n");
                    }
                    sb.append("DNS del sistema:   ");
                    List<InetAddress> dns = lp.getDnsServers();
                    for (int i = 0; i < dns.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(dns.get(i).getHostAddress());
                    }
                    sb.append("\n");

                    ProxyInfo proxy = lp.getHttpProxy();
                    sb.append("Proxy del WiFi:    ")
                            .append(proxy != null ? proxy.getHost() + ":" + proxy.getPort() : "Ninguno")
                            .append("\n");
                }
            }

            try {
                WifiManager wm = (WifiManager)
                        ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    WifiInfo wi = wm.getConnectionInfo();
                    if (wi != null) {
                        String ssid = wi.getSSID();
                        if (ssid != null) ssid = ssid.replace("\"", "");
                        sb.append("SSID:              ").append(ssid != null ? ssid : "—").append("\n");
                        sb.append("BSSID:             ").append(wi.getBSSID() != null ? wi.getBSSID() : "—").append("\n");
                        sb.append("RSSI:              ").append(wi.getRssi()).append(" dBm\n");
                        sb.append("Velocidad link:    ").append(wi.getLinkSpeed()).append(" Mbps\n");
                    }
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            sb.append("Error obteniendo info de red: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n");

        // 8. Logs
        sb.append("═══ 8. ÚLTIMOS LOGS (hasta 300 líneas) ═══\n");
        try {
            List<LogBuffer.Entry> logs = LogBuffer.snapshot();
            int inicio = Math.max(0, logs.size() - 300);
            for (int i = inicio; i < logs.size(); i++) {
                sb.append(LogBuffer.formatEntry(logs.get(i))).append("\n");
            }
            sb.append("\nTotal de logs en buffer: ").append(logs.size()).append("\n");
        } catch (Exception e) {
            sb.append("Error obteniendo logs: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n");

        sb.append(line);
        sb.append("  FIN DEL REPORTE\n");
        sb.append(line);

        return sb.toString();
    }

    private static boolean isRooted() {
        String[] paths = {
                "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su",
                "/data/local/su", "/su/bin/su"
        };
        for (String p : paths) {
            if (new File(p).exists()) return true;
        }
        return false;
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
