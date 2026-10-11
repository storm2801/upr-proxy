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
import android.view.Gravity;
import android.view.ViewGroup;
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

    // Toast persistente que se cancela al mostrar otro (evita el efecto "cola")
    private Toast currentToast;

    private final Runnable dismissToast = () -> {
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    };

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

        // ---- ACTUALIZACIÓN ----
        findViewById(R.id.btnCheckUpdates).setOnClickListener(v -> {
            // Este SÍ usa toast largo (estándar)
            Toast.makeText(this, "Buscando actualizaciones…", Toast.LENGTH_SHORT).show();
            buscarActualizacion(true);
        });

        // ---- DIAGNÓSTICO ----
        findViewById(R.id.btnViewLogs).setOnClickListener(v -> {
            quickToast("Abriendo logs");
            startActivity(new Intent(this, LogsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btnTestConn).setOnClickListener(v -> {
            quickToast("Probando…");
            probarConexion();
        });

        findViewById(R.id.btnSystemInfo).setOnClickListener(v -> {
            quickToast("Mostrando info");
            mostrarSystemInfo();
        });

        findViewById(R.id.btnCopyLogs).setOnClickListener(v -> copiarLogs());

        // ---- VPN ----
        findViewById(R.id.btnForceReconnect).setOnClickListener(v -> forzarReconexion());

        // ---- DATOS ----
        findViewById(R.id.btnClearLogs).setOnClickListener(v -> {
            LogBuffer.clear();
            L.i("DevMenu", "Logs borrados desde menú dev");
            quickToast("Logs borrados");
        });

        findViewById(R.id.btnResetOnboarding).setOnClickListener(v -> {
            SecurePrefs.get(this).edit().putBoolean("onboarding_done", false).apply();
            quickToast("Onboarding se mostrará al abrir");
        });

        findViewById(R.id.btnResetPrefs).setOnClickListener(v -> confirmarResetPrefs());

        // ---- SALIR ----
        findViewById(R.id.btnExitDev).setOnClickListener(v -> confirmarSalida());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.post(refresher);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(refresher);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(dismissToast);
        if (currentToast != null) currentToast.cancel();
        super.onDestroy();
    }

    // ---------- Toast rápido (cancela el anterior) ----------

    /** Muestra un toast corto cancelando el anterior para que sea instantáneo. */
    private void quickToast(String msg) {
        ui.removeCallbacks(dismissToast);
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
        // Cancelarlo a los 900ms para que dure menos que un SHORT normal
        ui.postDelayed(dismissToast, 900);
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
                        () -> iniciarDescarga(info));
            } else if (mostrarSiAlDia) {
                quickToast("✅ Última versión");
            }
        });
    }

    private void iniciarDescarga(UpdateInfo info) {
        if (info.apkUrl == null || info.apkUrl.isEmpty()) {
            quickToast("URL no disponible");
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
                        quickToast("Descarga completa");
                        UpdateDialog.installApk(DevMenuActivity.this, apkFile);
                    }

                    @Override
                    public void onError(String message) {
                        try { prog.dismiss(); } catch (Exception ignored) {}
                        ErrorDialog.show(DevMenuActivity.this,
                                "Error al descargar",
                                message, "Entendido", null, null, null);
                    }
                });
    }

    // ---------- Diagnóstico ----------

    private void copiarLogs() {
        String txt = LogBuffer.fullText();
        if (txt.isEmpty()) {
            quickToast("No hay logs");
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("UPR Proxy logs", txt));
            quickToast("Logs copiados");
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
            quickToast("Puerto inválido");
            return;
        }

        TestConnection.run(host, port, user, pass, (ok, msg) ->
                ErrorDialog.show(DevMenuActivity.this,
                        ok ? "✅ Conexión OK" : "❌ Error",
                        msg, "Entendido", null, null, null));
    }

    private void mostrarSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Modelo: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
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
        sb.append("VPN: ").append(ProxyStatus.vpnRunning.get() ? "activa" : "inactiva").append("\n");
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
                    quickToast("Reconectando…");

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
                    quickToast("Credenciales borradas");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir del modo desarrollador")
                .setMessage("Se desactivará el menú dev.\nPara reactivarlo: 7 toques en la versión.")
                .setPositiveButton("Salir", (d, w) -> {
                    SecurePrefs.get(this).edit()
                            .putBoolean("developer_mode", false).apply();
                    L.i("DevMenu", "Modo desarrollador desactivado");
                    quickToast("Modo dev desactivado");
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
