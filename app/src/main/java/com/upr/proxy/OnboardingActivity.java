package com.upr.proxy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OnboardingActivity extends AppCompatActivity {

    private View slide1, slide2, slide3;
    private View dot1, dot2, dot3;
    private Button btnNext, btnSkip;
    private int current = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // FLAG_SECURE eliminado: se permiten capturas de pantalla
        setContentView(R.layout.activity_onboarding);

        slide1 = findViewById(R.id.slide1);
        slide2 = findViewById(R.id.slide2);
        slide3 = findViewById(R.id.slide3);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        btnNext.setOnClickListener(v -> {
            if (current < 2) mostrarSlide(current + 1);
            else terminar();
        });
        btnSkip.setOnClickListener(v -> terminar());

        actualizarIndicadores();
    }

    private void mostrarSlide(int index) {
        View oldV = getSlide(current);
        View newV = getSlide(index);
        newV.setVisibility(View.VISIBLE);
        newV.setAlpha(0f);
        newV.setTranslationX(80f);
        newV.animate().alpha(1f).translationX(0f).setDuration(280)
                .setInterpolator(new AccelerateDecelerateInterpolator()).start();
        oldV.animate().alpha(0f).translationX(-80f).setDuration(220)
                .withEndAction(() -> {
                    oldV.setVisibility(View.GONE);
                    oldV.setAlpha(1f);
                    oldV.setTranslationX(0f);
                }).start();
        current = index;
        actualizarIndicadores();
        btnNext.setText(current == 2 ? R.string.onboarding_finish : R.string.onboarding_next);
    }

    private View getSlide(int i) {
        return i == 0 ? slide1 : (i == 1 ? slide2 : slide3);
    }

    private void actualizarIndicadores() {
        dot1.setBackgroundResource(current == 0 ? R.drawable.dot_green : R.drawable.dot_gray);
        dot2.setBackgroundResource(current == 1 ? R.drawable.dot_green : R.drawable.dot_gray);
        dot3.setBackgroundResource(current == 2 ? R.drawable.dot_green : R.drawable.dot_gray);
    }

    private void terminar() {
        SecurePrefs.get(this).edit().putBoolean("onboarding_done", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
}
