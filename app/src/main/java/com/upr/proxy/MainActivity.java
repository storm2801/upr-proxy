package com.upr.proxy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "upr_proxy";
    private static final String KEY_DEV_MODE = "developer_mode";
    private static final int TAPS_TO_ENABLE = 7;
    private static final long TAP_TIMEOUT_MS = 5000;

    private EditText editServer, editPort, editUser, editPassword;
    private Button btnToggle;
    private TextView txtStatus, txtVersion;
    private SharedPreferences prefs;
    private boolean isConnected = false;

    // Contador para el truco de los 7 toques
    private int tapCount = 0;
    private long firstTapTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> vpnLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startVpnService();
                } else {
                    Toast.makeText(this, "Permiso VPN denegado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editServer = findViewById(R.id.editServer);
        editPort = findViewById(R.id.editPort);
        editUser = findViewById(R.id.editUser);
        editPassword = findViewById(R.id.editPassword);
        btnToggle = findViewById(R.id.btnToggle);
        txtStatus = findViewById(R.id.txtStatus);
        txtVersion = findViewById(R.id.txtVersion);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        mostrarVersion();
        configurarTapEnVersion();
        loadPrefs();

        btnToggle.setOnClickListener(v -> {
            if (isConnected) {
                stopVpnService();
            } else {
                savePrefs();
                Intent intent = VpnService.prepare(MainActivity.this);
                if (intent != null) {
                    vpnLauncher.launch(intent);
                } else {
                    startVpnService();
                }
            }
        });
    }

    private void mostrarVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            String texto = getString(R.string.version_prefix) + " " + pi.versionName;
            if (prefs.getBoolean(KEY_DEV_MODE, false)) {
                texto += " (dev)";
            }
            txtVersion.setText(texto);
        } catch (Exception e) {
            txtVersion.setText(getString(R.string.version_prefix) + " desconocida");
        }
    }

    /**
     * Maneja los toques sobre el texto de la versión:
     *   - Si el modo dev NO está activo: cuenta 7 toques en 5 segundos → activa.
     *   - Si el modo dev YA está activo: un toque abre la pantalla de Logs.
     */
    private void configurarTapEnVersion() {
        txtVersion.setOnClickListener(v -> {
            boolean devMode = prefs.getBoolean(KEY_DEV_MODE, false);

            if (devMode) {
                // Abrir pantalla de logs
                startActivity(new Intent(MainActivity.this, LogsActivity.class));
                return;
            }

            long now = System.currentTimeMillis();
            if (tapCount == 0 || (now - firstTapTime) > TAP_TIMEOUT_MS) {
                tapCount = 1;
                firstTapTime = now;
            } else {
                tapCount++;
            }

            int restantes = TAPS_TO_ENABLE - tapCount;
            if (restantes > 0 && restantes <= 3) {
                Toast.makeText(this, "Faltan " + restantes + " toques...",
                        Toast.LENGTH_SHORT).show();
            }

            if (tapCount >= TAPS_TO_ENABLE) {
                activarModoDesarrollador();
                tapCount = 0;
            }
        });
    }

    private void activarModoDesarrollador() {
        prefs.edit().putBoolean(KEY_DEV_MODE, true).apply();
        Toast.makeText(this,
                "🔓 Modo desarrollador activado.\nToca la versión para ver los logs.",
                Toast.LENGTH_LONG).show();
        L.i("MainActivity", "Modo desarrollador activado");
        mostrarVersion();
    }

    private void loadPrefs() {
        editServer.setText(prefs.getString("server", "proxy.upr.edu.cu"));
        editPort.setText(prefs.getString("port", "8080"));
        editUser.setText(prefs.getString("user", ""));
        editPassword.setText(prefs.getString("password", ""));
    }

    private void savePrefs() {
        prefs.edit()
                .putString("server", editServer.getText().toString().trim())
                .putString("port", editPort.getText().toString().trim())
                .putString("user", editUser.getText().toString().trim())
                .putString("password", editPassword.getText().toString().trim())
                .apply();
    }

    private void startVpnService() {
        Intent intent = new Intent(this, ProxyVpnService.class);
        intent.putExtra("server", editServer.getText().toString().trim());
        intent.putExtra("port", editPort.getText().toString().trim());
        intent.putExtra("user", editUser.getText().toString().trim());
        intent.putExtra("password", editPassword.getText().toString().trim());
        startForegroundService(intent);

        isConnected = true;
        btnToggle.setText(R.string.disconnect);
        btnToggle.setBackgroundTintList(getColorStateList(R.color.olive_dark));
        txtStatus.setText(R.string.status_connected);
        L.i("MainActivity", "VPN iniciada por el usuario");
    }

    private void stopVpnService() {
        stopService(new Intent(this, ProxyVpnService.class));
        isConnected = false;
        btnToggle.setText(R.string.connect);
        btnToggle.setBackgroundTintList(getColorStateList(R.color.olive));
        txtStatus.setText(R.string.status_disconnected);
        L.i("MainActivity", "VPN detenida por el usuario");
    }
}
