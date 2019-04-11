package io.svechnikov.telegramchart.chart.views;

import android.animation.ValueAnimator;
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

public class HorizontalAxisView extends View
        implements NavigationBoundsListener, NavigationStateListener {

    private static final float SCALE_THRESHOLD = 0.8f;

    private Axis axis;
    private NavigationBounds navigationBounds;
    private NavigationState state = NavigationState.IDLE;
    private int minVisibleItemsCount;
    private ScaledPoint[] scaledPoints;
    private float currentScale = -1;
    private int currentStep = -1;

    private final Map<String, Integer> textSizes = new HashMap<>();
    private final int horizontalPadding;
    private final ValueAnimator animator = ValueAnimator.ofFloat(0, 1)
            .setDuration(300);

    private final Paint textPaint;

    private final Rect rect = new Rect();

    public HorizontalAxisView(Context context) {
        this(context, null);
    }

    public HorizontalAxisView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public HorizontalAxisView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                for (int i = 0; i < axis.size(); i++) {
                    ScaledPoint point = scaledPoints[i];

                    if (i % currentStep != 0 && i % (currentStep / 2) == 0) {
                        float alpha;
                        alpha = animation.getAnimatedFraction();

                        if (currentScale < SCALE_THRESHOLD) {
                            alpha = 1 - alpha;
                        }
                        point.alpha = (int)(alpha * 255);
                    }
                }

                invalidate();
            }
        });
    }

    public void setMinVisibleItemsCount(int count) {
        minVisibleItemsCount = count;
    }

    public void setAxis(Axis axis) {
        this.axis = axis;
    }

    boolean isPowerOfTwo(int x) {
        return (x & (x - 1)) == 0;
    }

    private void startAnimator() {
        animator.cancel();
        animator.start();
    }

    private void updatePoints() {
        if (getWidth() == 0) {
            return;
        }
        if (scaledPoints == null) {
            scaledPoints = new ScaledPoint[axis.size()];
            for (int i = 0; i < scaledPoints.length; i++) {
                scaledPoints[i] = new ScaledPoint();
                scaledPoints[i].name = axis.values[i].shortName;
            }
        }

        float stepFraction = navigationBounds.getWidth() / minVisibleItemsCount;
        int step = (int)Math.ceil(stepFraction);

        // step must be a power of 2
        if (!isPowerOfTwo(step)) {
            step = (int)Math.pow(2, Math.ceil(Math.log(step) / Math.log(2)));
        }

        float scale;
        if (step == 1) {
            scale = 1;
        }
        else {
            scale = (step - stepFraction) / (step / 2f);
        }

        if (currentScale != -1 && currentStep == step) {
            if (scale < SCALE_THRESHOLD && currentScale >= SCALE_THRESHOLD) {
                startAnimator();
            }
            else if (scale >= SCALE_THRESHOLD && currentScale < SCALE_THRESHOLD) {
                startAnimator();
            }
        }
        if (currentStep != step) {
            animator.cancel();
            currentStep = step;
        }
        currentScale = scale;

        float scaleX = getWidth() / navigationBounds.getWidth();

        for (int i = 0; i < axis.size(); i++) {
            ScaledPoint point = scaledPoints[i];
            if (step > 1 && i % (step / 2) != 0) {
                point.alpha = 0;
                continue;
            }

            float alpha;

            if (i % step == 0) {
                alpha = 1;
            }
            else {
                if (animator.isStarted()) {
                    alpha = point.alpha;
                }
                else {
                    if (scale < SCALE_THRESHOLD) {
                        alpha = 0;
                    }
                    else {
                        alpha = 1;
                    }
                }
            }

            point.alpha = (int)(255 * alpha);
            point.position = i * scaleX + horizontalPadding;
        }
    }

    @Override @SuppressWarnings("ConstantConditions")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (scaledPoints == null) {
            updatePoints();
        }

        int width = getWidth();
        float itemWidth = width / navigationBounds.getWidth();
        float axisWidth = itemWidth * scaledPoints.length;
        float left = axisWidth * (navigationBounds.left / scaledPoints.length);

        float y = getHeight() / 2f;
        for (ScaledPoint point: scaledPoints) {
            if (point.alpha == 0) {
                continue;
            }
            float x = point.position - left;

            if (x + getTextSize(point.name) < 0) {
                continue;
            }

            if (x > width) {
                break;
            }

            textPaint.setAlpha(point.alpha);
            canvas.drawText(point.name, x, y, textPaint);
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
                updatePoints();
                break;
            case IDLE:
                updatePoints();
                break;
        }

        invalidate();
    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        this.state = state;

        invalidate();
    }

    private static class ScaledPoint {
        public String name;
        public int alpha;
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
