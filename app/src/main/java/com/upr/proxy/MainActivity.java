package com.upr.proxy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editServer, editPort, editUser, editPassword;
    private Button btnToggle;
    private TextView txtStatus, txtVersion;
    private SharedPreferences prefs;
    private boolean isConnected = false;

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

        mostrarVersion();

        prefs = getSharedPreferences("upr_proxy", MODE_PRIVATE);
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
            txtVersion.setText(texto);
        } catch (Exception e) {
            txtVersion.setText(getString(R.string.version_prefix) + " desconocida");
        }
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
    }

    private void stopVpnService() {
        stopService(new Intent(this, ProxyVpnService.class));
        isConnected = false;
        btnToggle.setText(R.string.connect);
        btnToggle.setBackgroundTintList(getColorStateList(R.color.olive));
        txtStatus.setText(R.string.status_disconnected);
    }
}
