package io.svechnikov.telegramchart.chart.views.verticalaxis;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;

public class VerticalAxisLinesView extends View
        implements VerticalAxisView {

    private final Paint linePaint = new Paint();
    private VerticalAxisViewPoint points[];

    private final int initialAlpha;

    public VerticalAxisLinesView(Context context) {
        this(context, null);
    }

    public VerticalAxisLinesView(Context context,
                                 @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VerticalAxisLinesView(Context context,
                                 @Nullable AttributeSet attrs,
                                 int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources r = context.getResources();

        int[] style = {R.attr.dividerColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        int dividerColor = ta.getColor(0, Color.GRAY);
        ta.recycle();

        int lineWidth = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width);

        initialAlpha = Color.alpha(dividerColor);

        linePaint.setColor(dividerColor);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeWidth(lineWidth);

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @Override
    public void setViewPoints(VerticalAxisViewPoint[] points) {
        this.points = points;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (points == null) {
            return;
        }

        for (VerticalAxisViewPoint point: points) {
            int alpha = point.getAlpha();
            if (alpha == 0) {
                continue;
            }

            float y = (int)point.getY();
            int width = getWidth();

            linePaint.setAlpha((int)(initialAlpha * (alpha / 255f)));
            canvas.drawLine(0, y, width, y, linePaint);
        }
    }
}
