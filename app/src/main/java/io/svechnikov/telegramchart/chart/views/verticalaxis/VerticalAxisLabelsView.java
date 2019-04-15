package io.svechnikov.telegramchart.chart.views.verticalaxis;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;

public class VerticalAxisLabelsView extends View
        implements VerticalAxisView {

    private boolean alignRight;
    private final Paint textPaint = new Paint();
    private final int lineWidth;
    private final int axisTextHeight;
    private final int paddingHorizontal;
    private final int backgroundPadding;
    private final Paint backgroundPaint = new Paint();
    private final Rect rect = new Rect();
    protected VerticalAxisViewPoint points[];
    private VerticalAxisState forcedState;
    private boolean forcedStateActive;
    private boolean labelsBackgroundEnabled;

    public VerticalAxisLabelsView(Context context) {
        this(context, null);
    }

    public VerticalAxisLabelsView(Context context,
                                  @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VerticalAxisLabelsView(Context context,
                                  @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources r = context.getResources();

        int[] style = {R.attr.chartAxisTextColor,
                R.attr.chartBackground};
        TypedArray ta = context.obtainStyledAttributes(style);
        int axisTextColor = ta.getColor(0, Color.GRAY);
        int chartBackground = ta.getColor(1, Color.WHITE);
        ta.recycle();

        backgroundPaint.setColor(chartBackground);
        lineWidth = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width);
        backgroundPadding = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_axis_text_padding);

        textPaint.setTextSize(r.getDimensionPixelSize(
                R.dimen.chart_main_chart_axis_text_size));
        textPaint.setColor(axisTextColor);
        textPaint.setSubpixelText(true);
        textPaint.setAntiAlias(true);

        Rect rect = new Rect();
        textPaint.getTextBounds("1", 0, 1, rect);
        axisTextHeight = rect.height();

        paddingHorizontal = r.getDimensionPixelSize(R.dimen.chart_padding_horizontal);

        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setAlignRight(boolean alignRight) {
        this.alignRight = alignRight;
    }

    public void setColor(int color) {
        textPaint.setColor(color);
    }

    public void setLabelsBackgroundEnabled(boolean labelsBackgroundEnabled) {
        this.labelsBackgroundEnabled = labelsBackgroundEnabled;
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

        float textPositionDeltaY = lineWidth + axisTextHeight * 0.8f;

        boolean animating = false;

        if (forcedStateActive) {
            for (int i = points.length / 2; i < points.length; i++) {
                animating = points[i].getAlpha() > 0;
                if (animating) {
                    break;
                }
            }
        }

        for (int i = 0; i < points.length; i++) {
            VerticalAxisViewPoint point = points[i];
            int alpha = point.getAlpha();
            if (alpha == 0) {
                continue;
            }
            String text;

            if (forcedStateActive) {
                if (!animating) {
                    text = forcedState.points[i];
                }
                else {
                    text = forcedState.points[i / 2];
                }
            }
            else {
                text = point.getText();
            }

            textPaint.getTextBounds(text, 0, text.length(), rect);

            float y = point.getY();
            textPaint.setAlpha(alpha);
            int labelBackgroundAlpha = 0;

            if (labelsBackgroundEnabled) {
                labelBackgroundAlpha = point.getLabelBackgroundAlpha();
            }

            if (labelBackgroundAlpha != 0) {
                backgroundPaint.setAlpha(labelBackgroundAlpha);
                if (alignRight) {
                    canvas.drawRect(
                            getWidth() - rect.width() -
                                    paddingHorizontal - backgroundPadding * 2,
                            y - rect.height() - textPositionDeltaY -
                                    backgroundPadding,
                            getWidth() - paddingHorizontal,
                            y - textPositionDeltaY + backgroundPadding, backgroundPaint);
                }
                else {
                    canvas.drawRect(paddingHorizontal - backgroundPadding,
                            y - rect.height() - textPositionDeltaY - backgroundPadding,
                            paddingHorizontal + rect.width() + backgroundPadding,
                            y - textPositionDeltaY + backgroundPadding, backgroundPaint);
                }
            }

            if (alignRight) {
                canvas.drawText(text,
                        getWidth() - paddingHorizontal - rect.width() - backgroundPadding,
                        y - textPositionDeltaY, textPaint);
            }
            else {
                canvas.drawText(text,
                        paddingHorizontal, y - textPositionDeltaY, textPaint);
            }
        }
    }

    public void forceValues(int min, int max) {
        forcedStateActive = min > -1 && max > -1;
        if (forcedStateActive) {
            if (forcedState == null) {
                forcedState = new VerticalAxisState(points.length / 2);
            }
            forcedState.updateState(min, max);
        }
    }
}
