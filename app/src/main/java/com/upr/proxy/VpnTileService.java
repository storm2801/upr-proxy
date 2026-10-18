package com.upr.proxy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

/**
 * Quick Settings Tile para Android 7+.
 * Aparece en el panel de notificaciones rápidas y permite
 * conectar/desconectar la VPN con un solo toque.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class VpnTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        L.i("VpnTile", "Tile añadido al panel");
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean running = ProxyStatus.vpnRunning.get();

        if (running) {
            L.i("VpnTile", "Deteniendo VPN desde Tile");
            Intent stop = new Intent(this, ProxyVpnService.class);
            stop.setAction(ProxyVpnService.ACTION_STOP);
            startService(stop);
        } else {
            L.i("VpnTile", "Iniciando VPN desde Tile");
            SharedPreferences prefs = SecurePrefs.get(this);
            Intent start = new Intent(this, ProxyVpnService.class);
            start.putExtra("server", prefs.getString("server", "proxy.upr.edu.cu"));
            start.putExtra("port", prefs.getString("port", "8080"));
            start.putExtra("user", prefs.getString("user", ""));
            start.putExtra("password", prefs.getString("password", ""));
            ContextCompat.startForegroundService(this, start);
        }

        // Actualizar el icono inmediatamente
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean running = ProxyStatus.vpnRunning.get();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(running ? "Conectado" : "Desconectado");
        }
        tile.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
