package com.upr.proxy;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consulta version.json en el repo público y compara con la versión instalada.
 * No requiere autenticación: el repo es público.
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/storm2801/upr-proxy/main/version.json";
    private static final int TIMEOUT_MS = 8000;

    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Handler ui = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(boolean hasUpdate, UpdateInfo info, String errorMessage);
    }

    public static void check(Context ctx, Callback cb) {
        pool.submit(() -> {
            try {
                // 1. Leer version.json remoto
                String json = httpGet(VERSION_URL);
                if (json == null || json.isEmpty()) {
                    ui.post(() -> cb.onResult(false, null, "No se pudo obtener la información de versión"));
                    return;
                }

                JSONObject obj = new JSONObject(json);
                UpdateInfo info = new UpdateInfo();
                info.versionCode = obj.optInt("versionCode", 0);
                info.versionName = obj.optString("versionName", "");
                info.apkUrl = obj.optString("apkUrl", "");
                info.changelog = obj.optString("changelog", "");
                info.mandatory = obj.optBoolean("mandatory", false);
                info.publishedAt = obj.optString("publishedAt", "");

                // 2. Comparar con la versión instalada
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
                int currentCode = pi.versionCode;

                L.i(TAG, "Versión instalada: " + currentCode
                        + " | Versión remota: " + info.versionCode);

                boolean hasUpdate = info.versionCode > currentCode;
                final UpdateInfo fInfo = info;
                ui.post(() -> cb.onResult(hasUpdate, fInfo, null));

            } catch (Exception e) {
                L.w(TAG, "Error consultando actualizaciones: " + e.getMessage());
                ui.post(() -> cb.onResult(false, null, "Error de red: " + e.getMessage()));
            }
        });
    }

    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code != 200) {
                L.w(TAG, "HTTP " + code);
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            return sb.toString();

        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
