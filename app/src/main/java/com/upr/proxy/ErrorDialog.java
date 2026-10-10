package com.upr.proxy;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ErrorDialog {

    public static void show(AppCompatActivity ctx, String title, String message,
                            String primaryText, Runnable onPrimary,
                            String secondaryText, Runnable onSecondary) {
        final Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_error, null);
        d.setContentView(v);

        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(d.getWindow().getAttributes());
            lp.width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.9);
            d.getWindow().setAttributes(lp);
        }

        TextView t = v.findViewById(R.id.dialogTitle);
        TextView m = v.findViewById(R.id.dialogMessage);
        com.google.android.material.button.MaterialButton bp = v.findViewById(R.id.dialogBtnPrimary);
        com.google.android.material.button.MaterialButton bs = v.findViewById(R.id.dialogBtnSecondary);

        t.setText(title);
        m.setText(message);

        bp.setText(primaryText);
        bp.setOnClickListener(x -> {
            d.dismiss();
            if (onPrimary != null) onPrimary.run();
        });

        if (secondaryText != null) {
            bs.setVisibility(View.VISIBLE);
            bs.setText(secondaryText);
            bs.setOnClickListener(x -> {
                d.dismiss();
                if (onSecondary != null) onSecondary.run();
            });
        }

        d.setCancelable(true);
        d.show();
    }
}
