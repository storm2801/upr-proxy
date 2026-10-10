package com.upr.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "upr_proxy";
    private static final String KEY_DEV_MODE = "developer_mode";
    private static final String KEY_AUTO_RECONNECT = "auto_reconnect";
    private static final int TAPS_TO_ENABLE = 7;
    private static final long TAP_TIMEOUT_MS = 5000;
    private static final long UI_REFRESH_MS = 1000;

    private EditText editServer, editPort, editUser, editPassword;
    private Button btnToggle, btnTest;
    private TextView txtStatusTitle, txtStatusServer, txtStatusConnections,
                     txtStatusBytes, txtStatusUptime, txtVersion;
    private android.view.View statusDot;

    private SharedPreferences prefs;
    private boolean isConnected = false;
    private boolean autoReconnectEnabled = false;

    // 7 toques
    private int tapCount = 0;
    private long firstTapTime = 0;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refreshStatus();
            ui.postDelayed(this, UI_REFRESH_MS);
        }
    };

    // Receptor para saber cuándo cambia el estado del servicio
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            syncWithService();
        }
    };

    // Monitor de red para auto-reconectar
    private ConnectivityManager.NetworkCallback netCallback;

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
        btnTest = findViewById(R.id.btnTest);
        txtStatusTitle = findViewById(R.id.txtStatusTitle);
        txtStatusServer = findViewById(R.id.txtStatusServer);
        txtStatusConnections = findViewById(R.id.txtStatusConnections);
        txtStatusBytes = findViewById(R.id.txtStatusBytes);
        txtStatusUptime = findViewById(R.id.txtStatusUptime);
        txtVersion = findViewById(R.id.txtVersion);
        statusDot = findViewById(R.id.statusDot);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        autoReconnectEnabled = prefs.getBoolean(KEY_AUTO_RECONNECT, true);

        mostrarVersion();
        configurarTapEnVersion();
        loadPrefs();

        btnToggle.setOnClickListener(v -> toggleConnection());
        btnTest.setOnClickListener(v -> probarConexion());

        // Sincronizar con el servicio por si estaba activo
        syncWithService();
        registrarMonitorRed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(ProxyVpnService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, f);
        }
        syncWithService();
        ui.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(stateReceiver); } catch (Exception ignored) {}
        ui.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        if (netCallback != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) cm.unregisterNetworkCallback(netCallback);
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    // ---------- Estado ----------

    private void toggleConnection() {
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
    }

    private void startVpnService() {
        Intent intent = new Intent(this, ProxyVpnService.class);
        intent.putExtra("server", editServer.getText().toString().trim());
        intent.putExtra("port", editPort.getText().toString().trim());
        intent.putExtra("user", editUser.getText().toString().trim());
        intent.putExtra("password", editPassword.getText().toString().trim());
        ContextCompat.startForegroundService(this, intent);

        isConnected = true;
        actualizarBotonYEstado();
        L.i("MainActivity", "VPN iniciada por el usuario");
    }

    private void stopVpnService() {
        stopService(new Intent(this, ProxyVpnService.class));
        isConnected = false;
        actualizarBotonYEstado();
        L.i("MainActivity", "VPN detenida por el usuario");
    }

    /** Lee el estado real del servicio VPN del sistema. */
    private boolean isVpnServiceRunning() {
        return ProxyStatus.vpnRunning.get();
    }

    private void syncWithService() {
        boolean activo = isVpnServiceRunning();
        if (activo != isConnected) {
            isConnected = activo;
            actualizarBotonYEstado();
        }
    }

    private void actualizarBotonYEstado() {
        if (isConnected) {
            btnToggle.setText(R.string.disconnect);
            btnToggle.setBackgroundTintList(getColorStateList(R.color.olive_dark));
            txtStatusTitle.setText(R.string.status_connected);
            statusDot.setBackground(dotDrawable(R.drawable.dot_green));
        } else {
            btnToggle.setText(R.string.connect);
            btnToggle.setBackgroundTintList(getColorStateList(R.color.olive));
            txtStatusTitle.setText(R.string.status_disconnected);
            statusDot.setBackground(dotDrawable(R.drawable.dot_red));
            txtStatusServer.setText("—");
            txtStatusConnections.setText("0");
            txtStatusBytes.setText("0 B");
            txtStatusUptime.setText("—");
        }
    }

    private Drawable dotDrawable(int res) {
        return ContextCompat.getDrawable(this, res);
    }

    private void refreshStatus() {
        boolean activo = isVpnServiceRunning();
        if (activo != isConnected) {
            isConnected = activo;
            actualizarBotonYEstado();
        }

        if (!activo) return;

        txtStatusServer.setText(ProxyStatus.serverHost + ":" + ProxyStatus.serverPort);
        txtStatusConnections.setText(String.valueOf(ProxyStatus.activeConnections.get()));
        txtStatusBytes.setText(ProxyStatus.formatBytes(ProxyStatus.bytesSent.get()));
        txtStatusUptime.setText(ProxyStatus.formatUptime(ProxyStatus.startedAt));

        // Cambiar el color del punto según actividad
        int conns = ProxyStatus.activeConnections.get();
        if (conns > 0) {
            statusDot.setBackground(dotDrawable(R.drawable.dot_green));
        } else {
            statusDot.setBackground(dotDrawable(R.drawable.dot_orange));
        }
    }

    // ---------- Probar conexión ----------

    private void probarConexion() {
        savePrefs();
        String host = editServer.getText().toString().trim();
        String portStr = editPort.getText().toString().trim();
        String user = editUser.getText().toString().trim();
        String pass = editPassword.getText().toString();

        if (host.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Rellena servidor y puerto primero", Toast.LENGTH_SHORT).show();
            return;
        }
        int port;
        try { port = Integer.parseInt(portStr); }
        catch (Exception e) {
            Toast.makeText(this, "Puerto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTest.setEnabled(false);
        btnTest.setText("Probando…");
        L.i("MainActivity", "Prueba de conexión → " + host + ":" + port);

        TestConnection.run(host, port, user, pass, (ok, msg) -> {
            btnTest.setEnabled(true);
            btnTest.setText("Probar conexión al proxy");
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            L.i("MainActivity", "Resultado prueba: " + msg);
        });
    }

    // ---------- Auto-reconexión ----------

    private void registrarMonitorRed() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest req = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        netCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                ui.post(() -> {
                    if (autoReconnectEnabled && isConnected && !isVpnServiceRunning()) {
                        L.i("MainActivity", "Red disponible, reconectando VPN…");
                        startVpnService();
                    }
                });
            }
        };

        try {
            cm.registerNetworkCallback(req, netCallback);
        } catch (Exception e) {
            L.w("MainActivity", "No se pudo registrar monitor de red: " + e.getMessage());
        }
    }

    // ---------- 7 toques / modo dev ----------

    private void mostrarVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            String texto = getString(R.string.version_prefix) + " " + pi.versionName;
            if (prefs.getBoolean(KEY_DEV_MODE, false)) texto += " (dev)";
            txtVersion.setText(texto);
        } catch (Exception e) {
            txtVersion.setText(getString(R.string.version_prefix) + " desconocida");
        }
    }

    private void configurarTapEnVersion() {
        txtVersion.setOnClickListener(v -> {
            boolean devMode = prefs.getBoolean(KEY_DEV_MODE, false);
            if (devMode) {
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
                Toast.makeText(this, "Faltan " + restantes + " toques...", Toast.LENGTH_SHORT).show();
            }
            if (tapCount >= TAPS_TO_ENABLE) {
                prefs.edit().putBoolean(KEY_DEV_MODE, true).apply();
                Toast.makeText(this, "🔓 Modo desarrollador activado.\nToca la versión para ver los logs.",
                        Toast.LENGTH_LONG).show();
                L.i("MainActivity", "Modo desarrollador activado");
                mostrarVersion();
                tapCount = 0;
            }
        });
    }

    // ---------- Preferencias ----------

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
                .putString("password", editPassword.getText().toString())
                .apply();
    }
}
