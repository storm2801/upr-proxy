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

        findViewById(R.id.btnViewLogs).setOnClickListener(v -> {
            startActivity(new Intent(this, LogsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btnCopyLogs).setOnClickListener(v -> copiarLogs());

        findViewById(R.id.btnTestConn).setOnClickListener(v -> probarConexion());

        findViewById(R.id.btnSystemInfo).setOnClickListener(v -> mostrarSystemInfo());

        findViewById(R.id.btnForceReconnect).setOnClickListener(v -> forzarReconexion());

        findViewById(R.id.btnClearLogs).setOnClickListener(v -> {
            LogBuffer.clear();
            L.i("DevMenu", "Logs borrados desde menú dev");
            Toast.makeText(this, "Logs borrados", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnResetPrefs).setOnClickListener(v -> confirmarResetPrefs());

        findViewById(R.id.btnResetOnboarding).setOnClickListener(v -> {
            SecurePrefs.get(this).edit().putBoolean("onboarding_done", false).apply();
            Toast.makeText(this, "Onboarding se mostrará al abrir la app",
                    Toast.LENGTH_SHORT).show();
        });

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

    // ---------- Estado en vivo ----------

    private void refreshStatus() {
        boolean vpn = ProxyStatus.vpnRunning.get();
        txtDevVpnState.setText("VPN: " + (vpn ? "🟢 conectada" : "🔴 desconectada"));

        txtDevConns.setText("Conexiones activas: " + ProxyStatus.activeConnections.get());

        long total = ProxyStatus.bytesSent.get() + ProxyStatus.bytesReceived.get();
        txtDevBytes.setText("Datos: " + ProxyStatus.formatBytes(total)
                + "  (↑ " + ProxyStatus.formatBytes(ProxyStatus.bytesSent.get())
                + "  ↓ " + ProxyStatus.formatBytes(ProxyStatus.bytesReceived.get()) + ")");

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            txtDevVersion.setText("Versión: " + pi.versionName + " (code " + pi.versionCode + ")");
        } catch (Exception e) {
            txtDevVersion.setText("Versión: —");
        }

        txtDevDevice.setText("Dispositivo: " + Build.MANUFACTURER + " " + Build.MODEL
                + "  •  Android " + Build.VERSION.RELEASE
                + "  •  " + Build.SUPPORTED_ABIS[0]);
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
        sb.append("\nServidor actual: ")
                .append(SecurePrefs.get(this).getString("server", "—"))
                .append(":")
                .append(SecurePrefs.get(this).getString("port", "—"))
                .append("\n");
        sb.append("Usuario: ")
                .append(SecurePrefs.get(this).getString("user", "—"))
                .append("\n\n");
        sb.append("Estado VPN: ")
                .append(ProxyStatus.vpnRunning.get() ? "activa" : "inactiva")
                .append("\n");
        sb.append("Conexiones totales (sesión): ")
                .append(ProxyStatus.totalConnections.get());

        new AlertDialog.Builder(this)
                .setTitle("Información del sistema")
                .setMessage(sb.toString())
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void forzarReconexion() {
        new AlertDialog.Builder(this)
                .setTitle("Forzar reconexión")
                .setMessage("Se detendrá la VPN y se volverá a iniciar con los datos guardados. ¿Continuar?")
                .setPositiveButton("Reconectar", (d, w) -> {
                    // Detener
                    Intent stop = new Intent(this, ProxyVpnService.class);
                    stop.setAction(ProxyVpnService.ACTION_STOP);
                    startService(stop);
                    L.i("DevMenu", "Forzando reconexión VPN");

                    // Rearrancar tras 2s
                    ui.postDelayed(() -> {
                        SharedPreferences p = SecurePrefs.get(this);
                        Intent start = new Intent(this, ProxyVpnService.class);
                        start.putExtra("server", p.getString("server", "proxy.upr.edu.cu"));
                        start.putExtra("port", p.getString("port", "8080"));
                        start.putExtra("user", p.getString("user", ""));
                        start.putExtra("password", p.getString("password", ""));
                        ContextCompat.startForegroundService(this, start);
                        Toast.makeText(this, "VPN reiniciada", Toast.LENGTH_SHORT).show();
                    }, 2000);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---------- Mantenimiento ----------

    private void confirmarResetPrefs() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar credenciales")
                .setMessage("Se borrarán el servidor, puerto, usuario y contraseña guardados. La VPN se detendrá.")
                .setPositiveButton("Borrar", (d, w) -> {
                    SecurePrefs.get(this).edit().clear().apply();

                    Intent stop = new Intent(this, ProxyVpnService.class);
                    stop.setAction(ProxyVpnService.ACTION_STOP);
                    startService(stop);

                    L.w("DevMenu", "Credenciales borradas por el usuario");
                    Toast.makeText(this, "Credenciales borradas", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir del modo desarrollador")
                .setMessage("Se desactivará el acceso a este menú y a los logs.\n\n"
                        + "Para volver a activarlo, toca 7 veces el texto de la versión "
                        + "en la pantalla principal.")
                .setPositiveButton("Salir", (d, w) -> {
                    SecurePrefs.get(this).edit()
                            .putBoolean("developer_mode", false)
                            .apply();
                    L.i("DevMenu", "Modo desarrollador desactivado");
                    Toast.makeText(this, "Modo desarrollador desactivado",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    overridePendingTransition(android.R.anim.fade_in,
                            android.R.anim.fade_out);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
