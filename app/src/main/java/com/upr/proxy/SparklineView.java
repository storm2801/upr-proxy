package com.upr.proxy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * Gráfica tipo sparkline que muestra la velocidad de los últimos 30 puntos.
 * Fondo con degradado verde y línea verde oscura.
 */
public class SparklineView extends View {

    private static final int POINTS = 30;
    private final float[] speeds = new float[POINTS];
    private int index = 0;
    private int filled = 0;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    public SparklineView(Context c) { super(c); init(); }
    public SparklineView(Context c, AttributeSet a) { super(c, a); init(); }
    public SparklineView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        linePaint.setColor(Color.parseColor("#107530"));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(Color.parseColor("#E5EFE9"));
        gridPaint.setStrokeWidth(1.5f);
        gridPaint.setStyle(Paint.Style.STROKE);
    }

    public void push(float value) {
        speeds[index] = value;
        index = (index + 1) % POINTS;
        if (filled < POINTS) filled++;
        invalidate();
    }

    public void clear() {
        for (int i = 0; i < POINTS; i++) speeds[i] = 0;
        index = 0;
        filled = 0;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float padding = 6f;
        float drawW = w - padding * 2;
        float drawH = h - padding * 2;
        float bottom = h - padding;

        // Líneas guía
        canvas.drawLine(padding, bottom - drawH / 2, w - padding, bottom - drawH / 2, gridPaint);
        canvas.drawLine(padding, bottom, w - padding, bottom, gridPaint);

        if (filled < 2) return;

        // Encontrar max
        float max = 1f;
        for (int i = 0; i < POINTS; i++) if (speeds[i] > max) max = speeds[i];
        max = max * 1.15f;

        // Construir path
        linePath.reset();
        fillPath.reset();

        int count = filled;
        float stepX = drawW / (POINTS - 1);
        float startOffset = (POINTS - count) * stepX;

        for (int i = 0; i < count; i++) {
            int realIdx = (index - count + i + POINTS * 2) % POINTS;
            float v = speeds[realIdx];
            float x = padding + startOffset + i * stepX;
            float y = bottom - (v / max) * drawH;
            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        // Cerrar el fill
        float lastX = padding + startOffset + (count - 1) * stepX;
        fillPath.lineTo(lastX, bottom);
        fillPath.close();

        // Degradado de fondo
        fillPaint.setShader(new LinearGradient(0, 0, 0, bottom,
                Color.parseColor("#809DC0A9"), Color.parseColor("#109DC0A9"),
                Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}
