package com.upr.proxy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DevMenuActivity extends AppCompatActivity {

    private static final long REFRESH_MS = 1000;

    private TextView txtDevVpnState, txtDevConns, txtDevBytes,
                     txtDevVersion, txtDevDevice;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            refreshStatus();
            ui.postDelayed(this, REFRESH_MS);
        }
    };

    // ---------- Estado del flujo de actualización ----------
    private UpdateInfo pendingUpdate = null;
    private boolean waitingInstallPermission = false;
    private boolean hasResumedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_dev_menu);

        txtDevVpnState = findViewById(R.id.txtDevVpnState);
        txtDevConns = findViewById(R.id.txtDevConns);
        txtDevBytes = findViewById(R.id.txtDevBytes);
        txtDevVersion = findViewById(R.id.txtDevVersion);
        txtDevDevice = findViewById(R.id.txtDevDevice);

        // ACTUALIZACIÓN
        findViewById(R.id.btnCheckUpdates).setOnClickListener(v -> {
            L.i("DevMenu", "→ Buscar actualizaciones");
            Toast.makeText(this, "Buscando actualizaciones…", Toast.LENGTH_SHORT).show();
            buscarActualizacion(true);
        });

        // DIAGNÓSTICO
        findViewById(R.id.btnViewLogs).setOnClickListener(v -> {
            L.i("DevMenu", "→ Abrir logs");
            try {
                startActivity(new Intent(this, LogsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            } catch (Exception e) {
                L.e("DevMenu", "Error abriendo logs", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnTestConn).setOnClickListener(v -> {
            L.i("DevMenu", "→ Probar conexión");
            probarConexion();
        });

        findViewById(R.id.btnSystemInfo).setOnClickListener(v -> {
            L.i("DevMenu", "→ Info sistema");
            mostrarSystemInfo();
        });

        findViewById(R.id.btnCopyLogs).setOnClickListener(v -> {
            L.i("DevMenu", "→ Copiar logs");
            copiarLogs();
        });

        // VPN
        findViewById(R.id.btnForceReconnect).setOnClickListener(v -> {
            L.i("DevMenu", "→ Forzar reconexión");
            forzarReconexion();
        });

        // DATOS
        findViewById(R.id.btnClearLogs).setOnClickListener(v -> {
            LogBuffer.clear();
            L.i("DevMenu", "Logs borrados");
            Toast.makeText(this, "Logs borrados", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnResetOnboarding).setOnClickListener(v -> {
            SecurePrefs.get(this).edit().putBoolean("onboarding_done", false).apply();
            L.i("DevMenu", "Onboarding reactivado");
            Toast.makeText(this, "Onboarding se mostrará al abrir",
                    Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnResetPrefs).setOnClickListener(v -> {
            L.i("DevMenu", "→ Borrar credenciales");
            confirmarResetPrefs();
        });

        // SALIR
        findViewById(R.id.btnExitDev).setOnClickListener(v -> {
            L.i("DevMenu", "→ Salir modo dev");
            confirmarSalida();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.post(refresher);

        // Si estábamos esperando a que el usuario concediera el permiso de
        // instalación (volviendo de Ajustes), comprobamos si ya lo concedió.
        if (hasResumedOnce && waitingInstallPermission && pendingUpdate != null) {
            boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                    || getPackageManager().canRequestPackageInstalls();

            if (granted) {
                L.i("DevMenu", "Permiso de instalación concedido, retomando descarga");
                waitingInstallPermission = false;
                UpdateInfo info = pendingUpdate;
                pendingUpdate = null;
                descargarEInstalar(info);
            } else {
                L.w("DevMenu", "Permiso de instalación aún no concedido");
                waitingInstallPermission = false;
                pendingUpdate = null;
                Toast.makeText(this,
                        "Permiso no concedido. Pulsa otra vez para reintentar.",
                        Toast.LENGTH_LONG).show();
            }
        }
        hasResumedOnce = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(refresher);
    }

    // ---------- Estado en vivo ----------

    private void refreshStatus() {
        boolean vpn = ProxyStatus.vpnRunning.get();
        txtDevVpnState.setText("VPN: " + (vpn ? "🟢 activa" : "🔴 inactiva"));
        txtDevConns.setText("Conexiones: " + ProxyStatus.activeConnections.get());

        long total = ProxyStatus.bytesSent.get() + ProxyStatus.bytesReceived.get();
        txtDevBytes.setText("Datos: " + ProxyStatus.formatBytes(total)
                + "  ↑" + ProxyStatus.formatBytes(ProxyStatus.bytesSent.get())
                + "  ↓" + ProxyStatus.formatBytes(ProxyStatus.bytesReceived.get()));

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            txtDevVersion.setText("Versión: " + pi.versionName
                    + " (" + pi.versionCode + ")");
        } catch (Exception e) {
            txtDevVersion.setText("Versión: —");
        }

        txtDevDevice.setText("Dispositivo: " + Build.MANUFACTURER + " " + Build.MODEL
                + " · API " + Build.VERSION.SDK_INT);
    }

    // ---------- Actualización ----------

    private void buscarActualizacion(boolean mostrarSiAlDia) {
        UpdateChecker.check(this, (hasUpdate, info, err) -> {
            if (err != null) {
                ErrorDialog.show(DevMenuActivity.this,
                        "No se pudo comprobar",
                        err + "\n\nVerifica tu conexión a internet.",
                        "Entendido", null, null, null);
                return;
            }
            if (hasUpdate && info != null) {
                UpdateDialog.showAvailable(DevMenuActivity.this, info,
                        () -> lanzarActualizacion(info));
            } else if (mostrarSiAlDia) {
                Toast.makeText(this, "✅ Ya tienes la última versión",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Punto de entrada cuando el usuario acepta actualizar.
     * ORDEN CORRECTO:  permiso → descarga → instalación.
     */
    private void lanzarActualizacion(UpdateInfo info) {
        // 1. Comprobar permiso de instalación ANTES de descargar nada
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                L.i("DevMenu", "Falta permiso de instalación, abriendo Ajustes");
                pendingUpdate = info;
                waitingInstallPermission = true;
                UpdateDialog.showPermissionRequired(this);
                return;
            }
        }

        // 2. Permiso OK → descargar directamente
        descargarEInstalar(info);
    }

    private void descargarEInstalar(UpdateInfo info) {
        if (info.apkUrl == null || info.apkUrl.isEmpty()) {
            Toast.makeText(this, "URL de descarga no disponible",
                    Toast.LENGTH_LONG).show();
            return;
        }

        final android.app.Dialog prog = UpdateDialog.showProgress(this, info.versionName);

        UpdateDownloader.download(this, info.apkUrl, info.versionName,
                new UpdateDownloader.Callback() {
                    @Override
                    public void onProgress(int percent, long downloaded, long total) {
                        UpdateDialog.updateProgress(prog, percent, downloaded, total);
                    }

                    @Override
                    public void onComplete(java.io.File apkFile) {
                        try { prog.dismiss(); } catch (Exception ignored) {}
                        Toast.makeText(DevMenuActivity.this,
                                "Descarga completa. Abriendo instalador…",
                                Toast.LENGTH_SHORT).show();
                        // Permiso ya garantizado → instalar directamente
                        UpdateDialog.installApk(DevMenuActivity.this, apkFile);
                    }

                    @Override
                    public void onError(String message) {
                        try { prog.dismiss(); } catch (Exception ignored) {}
                        ErrorDialog.show(DevMenuActivity.this,
                                "Error al descargar",
                                "No se pudo descargar el APK:\n\n" + message,
                                "Entendido", null, null, null);
                    }
                });
    }

    // ---------- Diagnóstico ----------

    private void copiarLogs() {
        String txt = LogBuffer.fullText();
        if (txt.isEmpty()) {
            Toast.makeText(this, "No hay logs para copiar", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("UPR Proxy logs", txt));
            Toast.makeText(this, "Logs copiados (" + txt.length() + " caracteres)",
                    Toast.LENGTH_SHORT).show();

            ui.postDelayed(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        cm.clearPrimaryClip();
                    } else {
                        cm.setPrimaryClip(ClipData.newPlainText("", ""));
                    }
                } catch (Exception ignored) {}
            }, 60000);
        }
    }

    private void probarConexion() {
        SharedPreferences prefs = SecurePrefs.get(this);
        String host = prefs.getString("server", "proxy.upr.edu.cu");
        String portStr = prefs.getString("port", "8080");
        String user = prefs.getString("user", "");
        String pass = prefs.getString("password", "");

        int port;
        try { port = Integer.parseInt(portStr); }
        catch (Exception e) {
            Toast.makeText(this, "Puerto inválido guardado", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Probando…", Toast.LENGTH_SHORT).show();

        TestConnection.run(host, port, user, pass, (ok, msg) ->
                ErrorDialog.show(DevMenuActivity.this,
                        ok ? "✅ Conexión OK" : "❌ Error de conexión",
                        msg, "Entendido", null, null, null));
    }

    private void mostrarSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Modelo: ").append(Build.MANUFACTURER).append(" ")
                .append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Arquitectura: ");
        for (int i = 0; i < Build.SUPPORTED_ABIS.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(Build.SUPPORTED_ABIS[i]);
        }
        sb.append("\n\n");
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            sb.append("App: ").append(pi.packageName).append("\n");
            sb.append("Versión: ").append(pi.versionName)
                    .append(" (").append(pi.versionCode).append(")\n");
        } catch (Exception e) {
            sb.append("App: —\n");
        }
        sb.append("\nServidor: ")
                .append(SecurePrefs.get(this).getString("server", "—"))
                .append(":").append(SecurePrefs.get(this).getString("port", "—"))
                .append("\n");
        sb.append("Usuario: ")
                .append(SecurePrefs.get(this).getString("user", "—"))
                .append("\n\n");
        sb.append("VPN: ").append(ProxyStatus.vpnRunning.get() ? "activa" : "inactiva")
                .append("\n");
        sb.append("Conexiones (sesión): ").append(ProxyStatus.totalConnections.get());

        new AlertDialog.Builder(this)
                .setTitle("Información del sistema")
                .setMessage(sb.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void forzarReconexion() {
        new AlertDialog.Builder(this)
                .setTitle("Forzar reconexión")
                .setMessage("¿Detener y volver a iniciar la VPN?")
                .setPositiveButton("Reconectar", (d, w) -> {
                    Intent stop = new Intent(this, ProxyVpnService.class);
                    stop.setAction(ProxyVpnService.ACTION_STOP);
                    startService(stop);
                    L.i("DevMenu", "Forzando reconexión VPN");
                    Toast.makeText(this, "Reconectando…", Toast.LENGTH_SHORT).show();

                    ui.postDelayed(() -> {
                        SharedPreferences p = SecurePrefs.get(this);
                        Intent start = new Intent(this, ProxyVpnService.class);
                        start.putExtra("server", p.getString("server", "proxy.upr.edu.cu"));
                        start.putExtra("port", p.getString("port", "8080"));
                        start.putExtra("user", p.getString("user", ""));
                        start.putExtra("password", p.getString("password", ""));
                        ContextCompat.startForegroundService(this, start);
                    }, 2000);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- Mantenimiento ----------

    private void confirmarResetPrefs() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar credenciales")
                .setMessage("Se borrarán servidor, puerto, usuario y contraseña.")
                .setPositiveButton("Borrar", (d, w) -> {
                    SecurePrefs.get(this).edit().clear().apply();

                    Intent stop = new Intent(this, ProxyVpnService.class);
                    stop.setAction(ProxyVpnService.ACTION_STOP);
                    startService(stop);

                    L.w("DevMenu", "Credenciales borradas");
                    Toast.makeText(this, "Credenciales borradas",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir del modo desarrollador")
                .setMessage("Se desactivará el menú dev.\n\n"
                        + "Para reactivarlo: toca 7 veces el texto de la versión.")
                .setPositiveButton("Salir", (d, w) -> {
                    SecurePrefs.get(this).edit()
                            .putBoolean("developer_mode", false).apply();
                    L.i("DevMenu", "Modo desarrollador desactivado");
                    Toast.makeText(this, "Modo dev desactivado", Toast.LENGTH_SHORT).show();
                    ui.postDelayed(() -> {
                        finish();
                        overridePendingTransition(android.R.anim.fade_in,
                                android.R.anim.fade_out);
                    }, 400);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
