package com.upr.proxy;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Diálogos del sistema de actualización:
 *   1. showAvailable() → hay nueva versión → muestra changelog y botón Actualizar
 *   2. showProgress() → descarga con barra de progreso
 *   3. installApk() → abre el instalador de Android con el APK descargado
 */
public class UpdateDialog {

    // ---------- 1. Diálogo de versión disponible ----------

    public static void showAvailable(AppCompatActivity ctx, UpdateInfo info,
                                     Runnable onUpdatePressed) {
        new androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("📦 Nueva versión disponible")
                .setMessage(
                        "Versión instalada: " + getCurrentVersion(ctx) + "\n" +
                        "Versión nueva: " + info.versionName + "\n" +
                        "Publicada: " + info.publishedAt + "\n\n" +
                        "Cambios:\n" + info.changelog
                )
                .setPositiveButton("Actualizar", (d, w) -> onUpdatePressed.run())
                .setNegativeButton("Más tarde", null)
                .setCancelable(!info.mandatory)
                .show();
    }

    // ---------- 2. Diálogo de descarga con progreso ----------

    public static Dialog showProgress(AppCompatActivity ctx, String versionName) {
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_update_progress, null);
        d.setContentView(v);

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(d.getWindow().getAttributes());
            lp.width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.85);
            d.getWindow().setAttributes(lp);
        }

        d.setCancelable(false);
        d.show();

        TextView tv = v.findViewById(R.id.txtUpdateTitle);
        tv.setText("Descargando " + versionName);
        return d;
    }

    public static void updateProgress(Dialog d, int percent, long downloaded, long total) {
        if (d == null || !d.isShowing()) return;
        ProgressBar pb = d.findViewById(R.id.progressBar);
        TextView txt = d.findViewById(R.id.txtProgress);
        if (pb != null) pb.setProgress(percent);
        if (txt != null) {
            txt.setText(percent + "%  •  "
                    + ProxyStatus.formatBytes(downloaded) + " / "
                    + ProxyStatus.formatBytes(total));
        }
    }

    // ---------- 3. Instalar el APK ----------

    public static void installApk(AppCompatActivity ctx, File apkFile) {
        // En Android 8.0+ hay que pedir permiso para instalar apps desconocidas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.getPackageManager().canRequestPackageInstalls()) {
                // Abrir los ajustes para conceder el permiso
                new androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("Permiso necesario")
                        .setMessage("Para instalar la actualización, permite a UPR Proxy instalar apps desconocidas.")
                        .setPositiveButton("Abrir ajustes", (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            i.setData(Uri.parse("package:" + ctx.getPackageName()));
                            ctx.startActivity(i);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return;
            }
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".fileprovider", apkFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);

            Toast.makeText(ctx, "Sigue los pasos del instalador", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            L.e("UpdateDialog", "Error al abrir el instalador", e);
            Toast.makeText(ctx, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String getCurrentVersion(Context ctx) {
        try {
            return ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "—";
        }
    }
}
