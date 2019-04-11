package io.svechnikov.telegramchart.chart.views.horizontal_axis;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;

/**
 * Animations are not complete
 * Didn't have time to finish until deadline
 *
 * You can replace AnimatedHorizontalAxisView with NotAnimatedHorizontalAxisView,
 * which doesn't have fancy animations, but does its work great -
 * show horizontal axis with high precision
 */
public class AnimatedHorizontalAxisView2 extends View
        implements NavigationBoundsListener, NavigationStateListener {

    private Axis axis;
    private NavigationBounds navigationBounds;
    private NavigationState state = NavigationState.IDLE;
    private int minVisibleItemsCount;
    private final int horizontalPadding;
    private final Map<String, Integer> textSizes = new HashMap<>();

    private final Paint textPaint;

    private final Rect rect = new Rect();

    public AnimatedHorizontalAxisView2(Context context) {
        this(context, null);
    }

    public AnimatedHorizontalAxisView2(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public AnimatedHorizontalAxisView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources r = getResources();

        horizontalPadding = r.getDimensionPixelSize(R.dimen.chart_padding_horizontal);

        textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        textPaint.setSubpixelText(true);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(r.getDimensionPixelSize(
                R.dimen.chart_main_chart_axis_text_size));

        int[] style = {R.attr.chartAxisTextColor};

        TypedArray ta = context.obtainStyledAttributes(style);
        textPaint.setColor(ta.getColor(0, Color.GRAY));
        ta.recycle();
    }

    public void setMinVisibleItemsCount(int count) {
        minVisibleItemsCount = count;
    }

    public void setAxis(Axis axis) {
        this.axis = axis;
    }

    private void createScaledPoints() {

    }

    private Axis.Point[] intraPoints;
    private void createIntraPoints() {
        int count = Math.min(axis.size(), minVisibleItemsCount);
        intraPoints = new Axis.Point[count];
        float step = (float)axis.size() / minVisibleItemsCount;

        for (int i = 0; i < minVisibleItemsCount - 1; i++) {
            int index = Math.round(i * step);
            intraPoints[i] = axis.values[index];
        }

        intraPoints[minVisibleItemsCount - 1] = axis.values[axis.size() - 1];
    }

    @Override @SuppressWarnings("ConstantConditions")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (intraPoints == null) {
            createIntraPoints();
        }

        float y = getHeight() / 2f;
        float widthScale = 1 / (navigationBounds.getWidth() / axis.size());
        float elementWidth = getElementWidth() * widthScale;
        float fullWidth = elementWidth * minVisibleItemsCount - elementWidth;
        float offset = fullWidth * (navigationBounds.left / axis.size()) +
                (elementWidth - getTextSize(intraPoints[0].shortName)) / 2 - horizontalPadding;

        for (int i = 0; i < intraPoints.length; i++) {
            String text = intraPoints[i].shortName;
            int textSize = getTextSize(text);
            float x = i * elementWidth - offset + (elementWidth - textSize) / 2;

            canvas.drawText(text, x, y, textPaint);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private int getTextSize(String text) {
        if (!textSizes.containsKey(text)) {
            textPaint.getTextBounds(text, 0, text.length(), rect);
            textSizes.put(text, rect.width());
        }
        return textSizes.get(text);
    }

    @Override
    public void onNavigationBoundsChanged(NavigationBounds bounds) {
        navigationBounds = bounds;

        switch (state) {
            case MOVING_LEFT_BOUND:
            case MOVING_RIGHT_BOUND:
                createScaledPoints();
                break;
            case IDLE:
                createScaledPoints();
                break;
        }

        invalidate();
    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        this.state = state;

        invalidate();
    }

    private float getElementWidth() {
        return (float)getWidth() / minVisibleItemsCount;
    }

    private static class ScaledPoint {
        public String name;
        public float alpha;
        public float position;

        @Override
        public String toString() {
            return "ScaledPoint{" +
                    "name='" + name + '\'' +
                    ", alpha=" + alpha +
                    ", position=" + position +
                    '}';
        }
    }

}
