package com.upr.proxy;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Descarga el APK de actualización en /cache/updates/.
 */
public class UpdateDownloader {

    private static final String TAG = "UpdateDownloader";
    private static final int TIMEOUT_MS = 15000;
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Handler ui = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onProgress(int percent, long bytesDownloaded, long totalBytes);
        void onComplete(File apkFile);
        void onError(String message);
    }

    public static void download(Context ctx, String apkUrl, String versionName, Callback cb) {
        pool.submit(() -> {
            HttpURLConnection conn = null;
            try {
                File dir = new File(ctx.getCacheDir(), "updates");
                if (!dir.exists()) dir.mkdirs();

                File out = new File(dir, "UPRProxy-" + versionName + ".apk");
                if (out.exists()) out.delete();

                URL url = new URL(apkUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Accept", "application/vnd.android.package-archive");

                int code = conn.getResponseCode();
                if (code != 200) {
                    ui.post(() -> cb.onError("HTTP " + code));
                    return;
                }

                long total = conn.getContentLengthLong();
                L.i(TAG, "Descargando " + (total / 1024 / 1024) + " MB");

                try (InputStream in = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(out)) {

                    byte[] buf = new byte[8192];
                    long downloaded = 0;
                    int n;
                    int lastPercent = -1;

                    while ((n = in.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                        downloaded += n;
                        if (total > 0) {
                            int pct = (int) ((downloaded * 100) / total);
                            if (pct != lastPercent) {
                                lastPercent = pct;
                                final int fpct = pct;
                                final long fdown = downloaded;
                                ui.post(() -> cb.onProgress(fpct, fdown, total));
                            }
                        }
                    }
                }

                final File fOut = out;
                ui.post(() -> cb.onComplete(fOut));

            } catch (Exception e) {
                L.e(TAG, "Error descargando APK", e);
                ui.post(() -> cb.onError(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}
