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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UpdateDialog {

    // ---------- 1. Diálogo de nueva versión disponible ----------

    public static void showAvailable(AppCompatActivity ctx, UpdateInfo info,
                                     Runnable onUpdatePressed) {
        final Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_update_available, null);
        d.setContentView(v);

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(d.getWindow().getAttributes());
            lp.width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.9);
            d.getWindow().setAttributes(lp);
        }

        TextView tvCurrent = v.findViewById(R.id.tvCurrentVersion);
        TextView tvNew = v.findViewById(R.id.tvNewVersion);
        TextView tvDate = v.findViewById(R.id.tvPublishDate);
        TextView tvChangelog = v.findViewById(R.id.tvChangelog);
        MaterialButton btnNow = v.findViewById(R.id.btnUpdateNow);
        MaterialButton btnLater = v.findViewById(R.id.btnUpdateLater);

        tvCurrent.setText(getCurrentVersion(ctx));
        tvNew.setText(info.versionName);
        tvDate.setText(formatDate(info.publishedAt));
        tvChangelog.setText(formatChangelog(info.changelog));

        btnNow.setOnClickListener(x -> {
            d.dismiss();
            onUpdatePressed.run();
        });
        btnLater.setOnClickListener(x -> d.dismiss());

        d.setCancelable(!info.mandatory);
        d.show();
    }

    /** Convierte "2026-10-10" en "Publicada el 10 de octubre de 2026". */
    private static String formatDate(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = in.parse(iso);
            SimpleDateFormat out = new SimpleDateFormat("d 'de' MMMM 'de' yyyy",
                    new Locale("es", "ES"));
            return "Publicada el " + out.format(d);
        } catch (Exception e) {
            return "Publicada el " + iso;
        }
    }

    /** Normaliza el changelog: asegura que cada línea empiece con "• ". */
    private static String formatChangelog(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "• Mejoras de rendimiento y estabilidad.";
        }
        String[] lines = raw.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            String t = l.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("•") || t.startsWith("-") || t.startsWith("*")) {
                sb.append("• ").append(t.replaceFirst("^[•\\-\\*]\\s*", "")).append('\n');
            } else {
                sb.append("• ").append(t).append('\n');
            }
        }
        // Quitar salto final
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    // ---------- 2. Diálogo pidiendo permiso para instalar APKs ----------

    public static void showPermissionRequired(AppCompatActivity ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("Permiso necesario")
                .setMessage(
                    "Para instalar la actualización, primero debes permitir "
                    + "que UPR Proxy instale apps desconocidas.\n\n"
                    + "Se abrirá la pantalla de Ajustes. Activa el interruptor y "
                    + "vuelve a esta app: la descarga empezará automáticamente."
                )
                .setPositiveButton("Abrir Ajustes", (d, w) -> openInstallSettings(ctx))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private static void openInstallSettings(AppCompatActivity ctx) {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            i.setData(Uri.parse("package:" + ctx.getPackageName()));
            ctx.startActivity(i);
        } catch (Exception e) {
            try {
                ctx.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Exception e2) {
                Toast.makeText(ctx, "No se pudo abrir Ajustes", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ---------- 3. Diálogo de descarga con progreso ----------

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

    // ---------- 4. Instalar el APK ----------

    public static void installApk(AppCompatActivity ctx, File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ctx.getPackageManager().canRequestPackageInstalls()) {
                showPermissionRequired(ctx);
                return;
            }
        }

        try {
            Uri apkUri = FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".fileprovider", apkFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);

        } catch (Exception e) {
            L.e("UpdateDialog", "Error al abrir el instalador", e);
            Toast.makeText(ctx, "Error al abrir el instalador: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
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
