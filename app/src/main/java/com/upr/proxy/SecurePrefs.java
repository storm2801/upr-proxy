package com.upr.proxy;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Almacén cifrado para credenciales del proxy.
 * Usa EncryptedSharedPreferences con AES256-GCM y MasterKey del keystore
 * de Android. Si el dispositivo se compromete, las credenciales siguen
 * cifradas con una clave que solo el sistema conoce.
 *
 * Si el cifrado falla (dispositivo muy antiguo), cae a SharedPreferences
 * normales para no romper la app.
 */
public class SecurePrefs {

    private static final String FILE_NAME = "upr_proxy_secure";
    private static final String FALLBACK_FILE = "upr_proxy";
    private static SharedPreferences instance;

    public static synchronized SharedPreferences get(Context ctx) {
        if (instance != null) return instance;

        try {
            MasterKey masterKey = new MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            instance = EncryptedSharedPreferences.create(
                    ctx,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            L.i("SecurePrefs", "Almacén cifrado listo");

        } catch (Exception e) {
            L.w("SecurePrefs", "Cifrado no disponible, fallback a SharedPreferences: "
                    + e.getMessage());
            instance = ctx.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE);
        }

        return instance;
    }

    /** Migra datos viejos (texto plano) al almacén cifrado si aún no se hizo. */
    public static void migrateIfNeeded(Context ctx) {
        SharedPreferences secure = get(ctx);
        if (secure.getBoolean("__migrated", false)) return;

        SharedPreferences old = ctx.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE);
        String oldServer = old.getString("server", null);
        if (oldServer != null && !secure.contains("server")) {
            SharedPreferences.Editor e = secure.edit();
            e.putString("server", oldServer);
            e.putString("port", old.getString("port", "8080"));
            e.putString("user", old.getString("user", ""));
            e.putString("password", old.getString("password", ""));
            e.putBoolean("developer_mode", old.getBoolean("developer_mode", false));
            e.putBoolean("__migrated", true);
            e.apply();

            // Borrar el archivo antiguo (ya no lo necesitamos)
            old.edit().clear().apply();

            L.i("SecurePrefs", "Datos migrados a almacén cifrado");
        } else {
            secure.edit().putBoolean("__migrated", true).apply();
        }
    }
}
