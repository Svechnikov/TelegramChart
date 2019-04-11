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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;

public class NotAnimatedHorizontalAxisView extends View
        implements NavigationBoundsListener, NavigationStateListener {

    private Axis axis;
    private Axis scaledAxis;
    private NavigationBounds navigationBounds;
    private NavigationState state = NavigationState.IDLE;
    private int minVisibleItemsCount;
    private final Map<String, Integer> textSizes = new HashMap<>();

    private final Paint textPaint;

    private final Rect rect = new Rect();

    public NotAnimatedHorizontalAxisView(Context context) {
        this(context, null);
    }

    public NotAnimatedHorizontalAxisView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NotAnimatedHorizontalAxisView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources r = getResources();

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
        List<Axis.Point> points = new ArrayList<>();

        float stepFraction = navigationBounds.getWidth() / minVisibleItemsCount;
        int elementsCount = (int)(axis.size() / stepFraction);

        for (float i = 0; i < elementsCount; i++) {
            int index = (int)Math.round(stepFraction * i);
            points.add(axis.values[index]);
        }

        Axis.Point lastElement = axis.values[axis.size() - 1];
        if (!points.get(elementsCount - 1).equals(lastElement)) {
            if (navigationBounds.getWidth() == axis.size()) {
                points.remove(points.size() - 1);
            }
            points.add(lastElement);
        }

        Axis.Point[] axisPoints = new Axis.Point[points.size()];
        points.toArray(axisPoints);

        scaledAxis = new Axis(axisPoints);
    }

    @Override @SuppressWarnings("ConstantConditions")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (scaledAxis.size() == 0) {
            return;
        }

        float elementWidth = getElementWidth();
        float fullWidth = elementWidth * scaledAxis.size();
        float screenWidth = elementWidth * minVisibleItemsCount;
        float offsetPercent;
        float axisWidthDiff = axis.size() - navigationBounds.getWidth();

        if (axisWidthDiff > 0) {
            offsetPercent = navigationBounds.left / axisWidthDiff;
        }
        else {
            offsetPercent = 0;
        }

        float offset = (fullWidth - screenWidth) * offsetPercent;
        float firstElement = offset / elementWidth;
        int firstElementInt = Math.max((int)Math.floor(firstElement), 0);
        float firstElementFraction = -(firstElement - firstElementInt);
        float y = getHeight() / 2f;

        for (int i = firstElementInt; i < scaledAxis.size(); i++) {
            String text = scaledAxis.values[i].shortName;
            if (!textSizes.containsKey(text)) {
                textPaint.getTextBounds(text, 0, text.length(), rect);
                textSizes.put(text, rect.width());
            }
            int textSize = textSizes.get(text);
            int index = i - firstElementInt;
            int x = (int)Math.round(firstElementFraction * elementWidth +
                    index * elementWidth +
                    0.5 * (elementWidth - textSize));

            canvas.drawText(text, x, y, textPaint);
        }
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
                if (scaledAxis == null) {
                    createScaledPoints();
                }
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

}
