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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "upr_proxy";
    private static final String KEY_DEV_MODE = "developer_mode";
    private static final int TAPS_TO_ENABLE = 7;
    private static final long TAP_TIMEOUT_MS = 5000;
    private static final long UI_REFRESH_MS = 1000;

    private EditText editServer, editPort, editUser, editPassword;
    private Button btnToggle, btnTest;
    private android.widget.ProgressBar spinnerToggle;
    private com.upr.proxy.SparklineView sparkline;
    private TextView txtStatusTitle, txtStatusServer, txtStatusConnections,
                     txtStatusBytes, txtStatusSpeed, txtStatusUptime, txtVersion;
    private android.view.View statusDot;

    private SharedPreferences prefs;
    private boolean isConnected = false;

    private int tapCount = 0;
    private long firstTapTime = 0;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override public void run() {
            refreshStatus();
            ui.postDelayed(this, UI_REFRESH_MS);
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            syncWithService();
        }
    };

    private ConnectivityManager.NetworkCallback netCallback;

    private final ActivityResultLauncher<Intent> vpnLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    startVpnService();
                } else {
                    Toast.makeText(this, "Permiso VPN denegado", Toast.LENGTH_SHORT).show();
                    actualizarBotonYEstado();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        // Bloquear screenshots y vista previa en 'Apps recientes'
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        editServer = findViewById(R.id.editServer);
        editPort = findViewById(R.id.editPort);
        editUser = findViewById(R.id.editUser);
        editPassword = findViewById(R.id.editPassword);
        btnToggle = findViewById(R.id.btnToggle);
        spinnerToggle = findViewById(R.id.spinnerToggle);
        sparkline = findViewById(R.id.sparkline);
        btnTest = findViewById(R.id.btnTest);
        txtStatusTitle = findViewById(R.id.txtStatusTitle);
        txtStatusServer = findViewById(R.id.txtStatusServer);
        txtStatusConnections = findViewById(R.id.txtStatusConnections);
        txtStatusBytes = findViewById(R.id.txtStatusBytes);
        txtStatusSpeed = findViewById(R.id.txtStatusSpeed);
        txtStatusUptime = findViewById(R.id.txtStatusUptime);
        txtVersion = findViewById(R.id.txtVersion);
        statusDot = findViewById(R.id.statusDot);

        prefs = SecurePrefs.get(this);
        SecurePrefs.migrateIfNeeded(this);

        // ¿Primera vez? Mostrar onboarding
        if (!prefs.getBoolean("onboarding_done", false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }

        mostrarVersion();
        configurarTapEnVersion();
        loadPrefs();

        btnToggle.setOnClickListener(v -> toggleConnection());
        btnTest.setOnClickListener(v -> probarConexion());

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

    // ---------- Conexión ----------

    /**
     * Antes de conectar, verificamos que el proxy responde.
     * Si no responde, NO conectamos para evitar dejar al usuario
     * sin internet con una VPN activa pero inútil.
     */
    private void toggleConnection() {
        if (isConnected) {
            desconectar();
            return;
        }
        savePrefs();

        String host = editServer.getText().toString().trim();
        String portStr = editPort.getText().toString().trim();
        String user = editUser.getText().toString().trim();
        String pass = editPassword.getText().toString();

        if (host.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Rellena servidor y puerto", Toast.LENGTH_SHORT).show();
            return;
        }
        int port;
        try { port = Integer.parseInt(portStr); }
        catch (Exception e) {
            Toast.makeText(this, "Puerto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        btnToggle.setEnabled(false);
        btnToggle.setText("Verificando proxy…");
        if (spinnerToggle != null) spinnerToggle.setVisibility(android.view.View.VISIBLE);
        // Animación: el botón pulsa mientras verifica
        btnToggle.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120)
                .withEndAction(() -> btnToggle.animate().scaleX(1f).scaleY(1f)
                        .setDuration(180).start())
                .start();
        L.i("MainActivity", "Verificando proxy antes de conectar");

        TestConnection.run(host, port, user, pass, (ok, msg) -> {
            btnToggle.setEnabled(true);
            if (spinnerToggle != null) spinnerToggle.setVisibility(android.view.View.GONE);
            if (ok) {
                // Ahora sí, lanzar la VPN
                Intent intent = VpnService.prepare(MainActivity.this);
                if (intent != null) {
                    vpnLauncher.launch(intent);
                } else {
                    startVpnService();
                }
            } else {
                actualizarBotonYEstado();
                String titulo;
                String mensaje;
                if (msg.contains("Timeout")) {
                    titulo = "El proxy no responde";
                    mensaje = "El proxy de la UPR no contesta.\n\n"
                            + "Asegúrate de estar conectado al WiFi de la universidad y que "
                            + "el cable de red del aula esté bien enchufado.\n\n"
                            + "Si sigues con problemas, avisa al área de informatización.";
                } else if (msg.contains("resolver")) {
                    titulo = "No se encuentra el servidor";
                    mensaje = "No se pudo resolver '" + host + "'.\n\n"
                            + "Esto pasa normalmente cuando NO estás en la red de la UPR. "
                            + "Conéctate al WiFi de la universidad e inténtalo de nuevo.";
                } else if (msg.contains("Usuario o contraseña")) {
                    titulo = "Credenciales incorrectas";
                    mensaje = "El proxy rechazó tu usuario y contraseña.\n\n"
                            + "Revisa que:\n"
                            + "• El usuario esté bien escrito (sin espacios)\n"
                            + "• La contraseña sea la del proxy (no la del correo)\n"
                            + "• Las mayúsculas/minúsculas coincidan";
                } else if (msg.contains("conectar a")) {
                    titulo = "No se puede llegar al proxy";
                    mensaje = "No hubo forma de conectar con " + host + ":" + port + ".\n\n"
                            + "Verifica tu conexión al WiFi de la UPR.";
                } else {
                    titulo = "No se puede conectar";
                    mensaje = msg + "\n\nAsegúrate de estar conectado a la red de la UPR.";
                }

                ErrorDialog.show(MainActivity.this, titulo, mensaje,
                        getString(R.string.error_close), null,
                        null, null);
                L.w("MainActivity", "Cancelado: " + msg);
            }
        });
    }

    private void startVpnService() {
        Intent intent = new Intent(this, ProxyVpnService.class);
        intent.putExtra("server", editServer.getText().toString().trim());
        intent.putExtra("port", editPort.getText().toString().trim());
        intent.putExtra("user", editUser.getText().toString().trim());
        intent.putExtra("password", editPassword.getText().toString());
        ContextCompat.startForegroundService(this, intent);

        isConnected = true;
        actualizarBotonYEstado();
        L.i("MainActivity", "VPN iniciada");
    }

    /**
     * Envía una acción STOP explícita al servicio en lugar de stopService().
     * stopService() puede no llamar a onDestroy() a tiempo si el servicio
     * está ocupado; la acción STOP se procesa en onStartCommand y detiene
     * todo inmediatamente.
     */
    private void desconectar() {
        L.i("MainActivity", "Solicitando desconexión");
        Intent i = new Intent(this, ProxyVpnService.class);
        i.setAction(ProxyVpnService.ACTION_STOP);
        startService(i);

        // Optimista: reflejarlo en la UI ya
        isConnected = false;
        actualizarBotonYEstado();

        // Watchdog: si en 3 s sigue activo, usar stopService como plan B
        ui.postDelayed(() -> {
            if (ProxyStatus.vpnRunning.get()) {
                L.w("MainActivity", "STOP no funcionó, intentando stopService");
                stopService(new Intent(MainActivity.this, ProxyVpnService.class));
                ui.postDelayed(() -> {
                    if (ProxyStatus.vpnRunning.get()) {
                        L.e("MainActivity", "El servicio no se detiene");
                        // Forzamos la UI a mostrar desconectado aunque el
                        // sistema mantenga el servicio un rato.
                        ProxyStatus.vpnRunning.set(false);
                        actualizarBotonYEstado();
                        Toast.makeText(MainActivity.this,
                                "El servicio tardó en detenerse. Si sigue activo, reinicia el teléfono.",
                                Toast.LENGTH_LONG).show();
                    }
                }, 3000);
            }
        }, 3000);
    }

    // ---------- Estado en vivo ----------

    private void syncWithService() {
        boolean activo = ProxyStatus.vpnRunning.get();
        if (activo != isConnected) {
            isConnected = activo;
            actualizarBotonYEstado();
        }
    }

    private void actualizarBotonYEstado() {
        // Animación de entrada
        btnToggle.animate().scaleX(1.05f).scaleY(1.05f).setDuration(140)
                .withEndAction(() -> btnToggle.animate().scaleX(1f).scaleY(1f)
                        .setDuration(140).start())
                .start();

        if (isConnected) {
            btnToggle.setText(R.string.disconnect);
            btnToggle.setBackgroundTintList(getColorStateList(R.color.olive_dark));
            txtStatusTitle.setText(R.string.status_connected);
            statusDot.setBackground(dotDrawable(R.drawable.dot_green));
            // Animación de pulso en el punto verde
            statusDot.animate().scaleX(1.4f).scaleY(1.4f).setDuration(400)
                    .withEndAction(() -> statusDot.animate().scaleX(1f).scaleY(1f)
                            .setDuration(400).start())
                    .start();
        } else {
            btnToggle.setText(R.string.connect);
            btnToggle.setBackgroundTintList(getColorStateList(R.color.olive));
            txtStatusTitle.setText(R.string.status_disconnected);
            statusDot.setBackground(dotDrawable(R.drawable.dot_red));
            txtStatusServer.setText("—");
            txtStatusConnections.setText("0");
            txtStatusBytes.setText("0 B");
            txtStatusSpeed.setText("0 B/s");
            txtStatusUptime.setText("—");
            if (sparkline != null) sparkline.clear();
        }
    }

    private Drawable dotDrawable(int res) {
        return ContextCompat.getDrawable(this, res);
    }

    private void refreshStatus() {
        boolean activo = ProxyStatus.vpnRunning.get();
        if (activo != isConnected) {
            isConnected = activo;
            actualizarBotonYEstado();
        }
        if (!activo) return;

        txtStatusServer.setText(ProxyStatus.serverHost + ":" + ProxyStatus.serverPort);
        txtStatusConnections.setText(String.valueOf(ProxyStatus.activeConnections.get()));
        txtStatusBytes.setText(ProxyStatus.formatBytes(ProxyStatus.bytesSent.get()));
        ProxyStatus.updateSpeed();
        txtStatusSpeed.setText("↑ " + ProxyStatus.formatSpeedUp()
                    + "  ↓ " + ProxyStatus.formatSpeedDown());
        if (sparkline != null) sparkline.push(ProxyStatus.speedBps);
        txtStatusUptime.setText(ProxyStatus.formatUptime(ProxyStatus.startedAt));

        int conns = ProxyStatus.activeConnections.get();
        statusDot.setBackground(dotDrawable(
                conns > 0 ? R.drawable.dot_green : R.drawable.dot_orange));
    }

    // ---------- Probar ----------

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

        TestConnection.run(host, port, user, pass, (ok, msg) -> {
            btnTest.setEnabled(true);
            btnTest.setText("Probar conexión al proxy");
            Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
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
            @Override public void onAvailable(Network network) {
                ui.post(() -> {
                    // Solo reconectar si el usuario tenía la VPN activa
                    // y el servicio realmente se cayó.
                    if (isConnected && !ProxyStatus.vpnRunning.get()) {
                        L.i("MainActivity", "Red disponible, reconectando VPN…");
                        startVpnService();
                    }
                });
            }
        };
        try { cm.registerNetworkCallback(req, netCallback); }
        catch (Exception e) { L.w("MainActivity", "NetCallback: " + e.getMessage()); }
    }

    // ---------- Modo desarrollador ----------

    private void mostrarVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            String texto = getString(R.string.version_prefix) + " " + pi.versionName;
            if (prefs.getBoolean(KEY_DEV_MODE, false)) texto += "  •  modo dev";
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
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return;
            }
            long now = System.currentTimeMillis();
            if (tapCount == 0 || (now - firstTapTime) > TAP_TIMEOUT_MS) {
                tapCount = 1; firstTapTime = now;
            } else { tapCount++; }
            int restantes = TAPS_TO_ENABLE - tapCount;
            if (restantes > 0 && restantes <= 3) {
                Toast.makeText(this, "Faltan " + restantes + " toques...", Toast.LENGTH_SHORT).show();
            }
            if (tapCount >= TAPS_TO_ENABLE) {
                prefs.edit().putBoolean(KEY_DEV_MODE, true).apply();
                Toast.makeText(this, "🔓 Modo desarrollador activado.\nToca la versión para ver los logs.",
                        Toast.LENGTH_LONG).show();
                // Vibración corta de confirmación
                try {
                    android.os.Vibrator vb = (android.os.Vibrator)
                            getSystemService(Context.VIBRATOR_SERVICE);
                    if (vb != null && vb.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= 26) {
                            vb.vibrate(android.os.VibrationEffect.createOneShot(
                                    80, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vb.vibrate(80);
                        }
                    }
                } catch (Exception ignored) {}
                // Animación del texto de versión
                txtVersion.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150)
                        .withEndAction(() -> txtVersion.animate()
                                .scaleX(1f).scaleY(1f).setDuration(150).start())
                        .start();
                mostrarVersion();
                tapCount = 0;
            }
        });
    }

    // ---------- Prefs ----------

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
