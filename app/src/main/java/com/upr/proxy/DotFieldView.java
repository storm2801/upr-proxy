package com.upr.proxy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;
import java.util.Random;

/**
 * Fondo animado de puntitos que cambian de color según "manchas"
 * invisibles que flotan tipo lámpara de lava.
 *
 * Valores aprobados:
 *   Velocidad 100 · Densidad 8 · Manchas 15 · Tamaño manchas 134 · Punto 4
 */
public class DotFieldView extends View {

    // --- Configuración fija (valores aprobados) ---
    private static final int    BLOB_COUNT  = 15;
    private static final float  BLOB_SIZE   = 134f;
    private static final int    DOT_SPACING = 8;
    private static final float  DOT_RADIUS  = 4f;
    private static final float  SPEED       = 1.0f;

    // Paleta ordenada de claro a oscuro (8 tonos)
    private static final int[] PALETTE = {
        0xFFF9FCFA,  // casi blanco
        0xFFD9E5DE,  // verde muy pálido
        0xFFB0D4BE,  // verde pálido
        0xFF80B894,  // verde suave
        0xFF4F8D64,  // verde medio
        0xFF2BB05B,  // verde vivo
        0xFF1A9442,  // verde intenso
        0xFF0E7030,  // verde profundo
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint();
    private final Random rnd = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Blob[] blobs;
    private float[] influenceBuffer;
    private int bufferCols, bufferRows;

    private float time = 0f;
    private long lastFrameNs = 0;
    private boolean running = false;

    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = (now - lastFrameNs) / 1_000_000_000f;
            if (dt > 0.1f) dt = 0.1f;
            if (dt < 0f)  dt = 0f;
            lastFrameNs = now;
            time += dt * (0.15f + SPEED * 1.1f);
            invalidate();
            postOnAnimation(this);
        }
    };

    public DotFieldView(Context c) { super(c); init(); }
    public DotFieldView(Context c, AttributeSet a) { super(c, a); init(); }
    public DotFieldView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        bgPaint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            createBlobs(w, h);
            influenceBuffer = null; // forzar recreación
        }
    }

    private void createBlobs(int w, int h) {
        blobs = new Blob[BLOB_COUNT];
        for (int i = 0; i < BLOB_COUNT; i++) blobs[i] = new Blob(w, h);
    }

    public void start() {
        if (running) return;
        running = true;
        lastFrameNs = System.nanoTime();
        postOnAnimation(frame);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(frame);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || blobs == null) return;

        // Fondo blanco
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Actualizar posiciones de blobs
        for (Blob b : blobs) b.update(time);

        // Preparar malla
        int cols = (int) Math.ceil((float) w / DOT_SPACING) + 2;
        int rows = (int) Math.ceil((float) h / DOT_SPACING) + 2;

        // Buffer de influencia
        int needed = cols * rows;
        if (influenceBuffer == null || influenceBuffer.length < needed) {
            influenceBuffer = new float[needed];
            bufferCols = cols;
            bufferRows = rows;
        } else {
            Arrays.fill(influenceBuffer, 0, needed, 0f);
        }

        // Sumar influencia de cada blob SOLO en su área (optimización clave)
        for (Blob b : blobs) {
            float r2 = b.r * b.r;
            int minC = Math.max(0, (int)((b.x - b.r) / DOT_SPACING) - 1);
            int maxC = Math.min(cols - 1, (int)((b.x + b.r) / DOT_SPACING) + 1);
            int minR = Math.max(0, (int)((b.y - b.r) / DOT_SPACING) - 1);
            int maxR = Math.min(rows - 1, (int)((b.y + b.r) / DOT_SPACING) + 1);

            for (int r = minR; r <= maxR; r++) {
                float rowOffset = (r % 2) * DOT_SPACING * 0.5f;
                float y = r * DOT_SPACING - DOT_SPACING;
                int base = r * cols;
                for (int c = minC; c <= maxC; c++) {
                    float x = c * DOT_SPACING + rowOffset - DOT_SPACING;
                    float dx = x - b.x, dy = y - b.y;
                    float d2 = dx*dx + dy*dy;
                    if (d2 < r2) {
                        float d = (float) Math.sqrt(d2);
                        float t = 1f - d / b.r;
                        float s = t * t * (3f - 2f * t) * b.strength;
                        influenceBuffer[base + c] += s;
                    }
                }
            }
        }

        // Dibujar puntos
        for (int r = 0; r < rows; r++) {
            float rowOffset = (r % 2) * DOT_SPACING * 0.5f;
            float y = r * DOT_SPACING - DOT_SPACING;
            int base = r * cols;
            for (int c = 0; c < cols; c++) {
                float x = c * DOT_SPACING + rowOffset - DOT_SPACING;

                float inf = influenceBuffer[base + c];
                if (inf > 1f) inf = 1f;

                float noise = (hash(c, r) - 0.5f) * 0.10f;
                float t = (float) Math.pow(inf, 0.72) + noise;
                if (t < 0f) t = 0f;
                if (t > 1f) t = 1f;

                paint.setColor(colorAt(t));
                canvas.drawCircle(x, y, DOT_RADIUS, paint);
            }
        }
    }

    // --- Interpolación de paleta ---
    private int colorAt(float t) {
        int n = PALETTE.length - 1;
        float x = t * n;
        int i = (int) Math.floor(x);
        float f = x - i;
        if (i >= n) return PALETTE[n];
        if (i < 0) return PALETTE[0];
        return lerpColor(PALETTE[i], PALETTE[i + 1], f);
    }

    private int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF,
            g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF,
            g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float hash(int x, int y) {
        double n = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453;
        return (float)(n - Math.floor(n));
    }

    // --- Blob (mancha invisible) ---
    private class Blob {
        float baseX, baseY, size;
        float px, py, ps;
        float vx, vy, vs;
        float ax, ay;
        float strength;
        float x, y, r;

        Blob(int w, int h) {
            baseX = rnd.nextFloat() * w;
            baseY = rnd.nextFloat() * h;
            size = BLOB_SIZE * (0.75f + rnd.nextFloat() * 0.5f);
            px = rnd.nextFloat() * (float)Math.PI * 2f;
            py = rnd.nextFloat() * (float)Math.PI * 2f;
            ps = rnd.nextFloat() * (float)Math.PI * 2f;
            vx = 0.12f + rnd.nextFloat() * 0.30f;
            vy = 0.12f + rnd.nextFloat() * 0.30f;
            vs = 0.15f + rnd.nextFloat() * 0.30f;
            ax = 40f + rnd.nextFloat() * 70f;
            ay = 40f + rnd.nextFloat() * 70f;
            strength = 0.55f + rnd.nextFloat() * 0.55f;
            x = baseX; y = baseY; r = size;
        }

        void update(float t) {
            x = baseX + (float)Math.sin(t * vx + px) * ax;
            y = baseY + (float)Math.cos(t * vy + py) * ay;
            r = size * (0.85f + 0.15f * (float)Math.sin(t * vs + ps));
        }
    }
}
