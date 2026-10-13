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
import android.net.Uri;
import android.provider.Settings;
import android.net.LinkProperties;
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

    // ---------- Estado del flujo de actualización ----------
    private UpdateInfo pendingUpdate = null;
    private boolean waitingInstallPermission = false;
    private boolean hasResumedOnce = false;

    // Toast rápido que se cancela solo para no encolarse
    private Toast currentToast;
    private final Runnable dismissToast = () -> {
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    };

    private void quickToast(String msg) {
        ui.removeCallbacks(dismissToast);
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        currentToast.show();
        ui.postDelayed(dismissToast, 900);
    }

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

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                L.i("MainActivity", "Permiso notificaciones: " + granted);
            });

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
        // FLAG_SECURE eliminado: se permiten capturas de pantalla
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

        // Pedir permiso de notificaciones (Android 13+)
        pedirPermisoNotificaciones();

        // Chequeo de actualizaciones al abrir la app
        comprobarActualizacionAlAbrir();
    }

    // ---------- Permisos ----------

    private void pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Verifica si el WiFi actual tiene un proxy HTTP configurado.
     * Si lo tiene, activar la VPN crea un "doble proxy" que rompe todo.
     * @return true si hay proxy configurado en el WiFi.
     */
    private boolean wifiTieneProxyConfigurado() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            LinkProperties lp = cm.getLinkProperties(active);
            if (lp == null) return false;
            return lp.getHttpProxy() != null;
        } catch (Exception e) {
            L.w("MainActivity", "Error comprobando proxy WiFi: " + e.getMessage());
            return false;
        }
    }

    // ---------- Chequeo de actualización al abrir la app ----------

    private void comprobarActualizacionAlAbrir() {
        UpdateChecker.check(this, (hasUpdate, info, err) -> {
            // Si no hay error y hay nueva versión → diálogo bonito
            if (err == null && hasUpdate && info != null) {
                UpdateDialog.showAvailable(MainActivity.this, info,
                        () -> lanzarActualizacion(info));
            }
            // Si no hay update o hay error → silencio total
        });
    }

    /**
     * Flujo correcto: permiso → descarga → instalación.
     */
    private void lanzarActualizacion(UpdateInfo info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                L.i("MainActivity", "Falta permiso de instalación, abriendo Ajustes");
                pendingUpdate = info;
                waitingInstallPermission = true;
                UpdateDialog.showPermissionRequired(this);
                return;
            }
        }
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
                        UpdateDialog.installApk(MainActivity.this, apkFile);
                    }

                    @Override
                    public void onError(String message) {
                        try { prog.dismiss(); } catch (Exception ignored) {}
                        ErrorDialog.show(MainActivity.this,
                                "Error al descargar",
                                message, "Entendido", null, null, null);
                    }
                });
    }

    // ---------- Ciclo de vida ----------

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(ProxyVpnService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stateReceiver, f);
        }
        mostrarVersion();
        syncWithService();
        ui.post(refreshRunnable);


        // Si venimos de conceder el permiso de instalación en Ajustes,
        // retomamos el flujo de actualización automáticamente.
        if (hasResumedOnce && waitingInstallPermission && pendingUpdate != null) {
            boolean granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                    || getPackageManager().canRequestPackageInstalls();

            if (granted) {
                L.i("MainActivity", "Permiso concedido, retomando descarga");
                waitingInstallPermission = false;
                UpdateInfo info = pendingUpdate;
                pendingUpdate = null;
                descargarEInstalar(info);
            } else {
                L.w("MainActivity", "Permiso aún no concedido");
                waitingInstallPermission = false;
                pendingUpdate = null;
                Toast.makeText(this,
                        "Permiso no concedido. Puedes intentarlo de nuevo más tarde.",
                        Toast.LENGTH_LONG).show();
            }
        }
        hasResumedOnce = true;
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

        // Verificar si el WiFi ya tiene proxy configurado (doble proxy rompe la app)
        if (wifiTieneProxyConfigurado()) {
            new AlertDialog.Builder(this)
                    .setTitle("Quita el proxy del WiFi")
                    .setMessage(
                        "Este WiFi tiene un proxy configurado manualmente.\n\n"
                        + "UPR Proxy necesita que el WiFi use conexión normal "
                        + "para enrutar el tráfico por su propia VPN.\n\n"
                        + "Ve a:\n"
                        + "  Ajustes → WiFi → UPR-WIFI → Proxy → Ninguno\n\n"
                        + "Luego vuelve a pulsar Conectar."
                    )
                    .setPositiveButton("Abrir Ajustes WiFi", (d, w) -> {
                        try {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        } catch (Exception ignored) {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }

        btnToggle.setEnabled(false);
        btnToggle.setText("Verificando proxy…");
        if (spinnerToggle != null) spinnerToggle.setVisibility(android.view.View.VISIBLE);
        btnToggle.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120)
                .withEndAction(() -> btnToggle.animate().scaleX(1f).scaleY(1f)
                        .setDuration(180).start())
                .start();
        L.i("MainActivity", "Verificando proxy antes de conectar");

        TestConnection.run(host, port, user, pass, (ok, msg) -> {
            btnToggle.setEnabled(true);
            if (spinnerToggle != null) spinnerToggle.setVisibility(android.view.View.GONE);
            if (ok) {
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

    private void desconectar() {
        L.i("MainActivity", "Solicitando desconexión");
        Intent i = new Intent(this, ProxyVpnService.class);
        i.setAction(ProxyVpnService.ACTION_STOP);
        startService(i);

        isConnected = false;
        actualizarBotonYEstado();

        ui.postDelayed(() -> {
            if (ProxyStatus.vpnRunning.get()) {
                L.w("MainActivity", "STOP no funcionó, intentando stopService");
                stopService(new Intent(MainActivity.this, ProxyVpnService.class));
                ui.postDelayed(() -> {
                    if (ProxyStatus.vpnRunning.get()) {
                        L.e("MainActivity", "El servicio no se detiene");
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
        btnToggle.animate().scaleX(1.05f).scaleY(1.05f).setDuration(140)
                .withEndAction(() -> btnToggle.animate().scaleX(1f).scaleY(1f)
                        .setDuration(140).start())
                .start();

        if (isConnected) {
            btnToggle.setText(R.string.disconnect);
            btnToggle.setBackgroundTintList(getColorStateList(R.color.olive_dark));
            txtStatusTitle.setText(R.string.status_connected);
            statusDot.setBackground(dotDrawable(R.drawable.dot_green));
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
            Toast.makeText(this, "Rellena servidor y puerto primero",
                    Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(MainActivity.this, DevMenuActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return;
            }
            long now = System.currentTimeMillis();
            if (tapCount == 0 || (now - firstTapTime) > TAP_TIMEOUT_MS) {
                tapCount = 1; firstTapTime = now;
            } else { tapCount++; }
            if (tapCount >= TAPS_TO_ENABLE) {
                prefs.edit().putBoolean(KEY_DEV_MODE, true).apply();
                quickToast("Modo desarrollador activado");
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
