package io.svechnikov.telegramchart.chart.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;

public class VerticalAxisView extends View
        implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private int pointsCount;

    private State currentState;
    private State nextState;
    private boolean animationCanceled;

    private int currentMaxValue;
    private float animatedFraction;
    private boolean stateInitialized;
    private boolean hasDrawn;

    private ValueAnimator animator;
    private final Paint linePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final int lineWidth;
    private final int axisTextHeight;
    private final int topPadding;
    private float itemHeight;
    private boolean animating;
    private final int paddingHorizontal;
    private int pendingMaxValue = -1;

    private static final float ANIMATION_START_THRESHOLD = 0.1f;
    private static final float ANIMATION_INTERRUPT_THRESHOLD = 0.8f;

    public VerticalAxisView(Context context) {
        this(context, null);
    }

    public VerticalAxisView(Context context,
                            @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VerticalAxisView(Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources r = context.getResources();

        topPadding = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_plot_top_padding);

        int[] style = {
                R.attr.dividerColor,
                R.attr.chartAxisTextColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        int dividerColor = ta.getColor(0, Color.GRAY);
        int axisTextColor = ta.getColor(1, Color.GRAY);
        ta.recycle();

        lineWidth = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width);

        linePaint.setColor(dividerColor);
        linePaint.setStrokeWidth(lineWidth);

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

    @Override
    public void onAnimationStart(Animator animation) {
        animating = true;
    }

    private void copyState(State state1, State state2) {
        state2.maxValue = state1.maxValue;
        System.arraycopy(state1.points, 1,
                state2.points, 1, pointsCount - 1);

        state1.maxValue = -1;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        animating = false;
        if (!animationCanceled) {
            copyState(nextState, currentState);
            if (pendingMaxValue != -1) {
                setMaxValue(pendingMaxValue);
            }
        }
        animationCanceled = false;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        animationCanceled = true;
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        currentMaxValue = (int)animation.getAnimatedValue();
        animatedFraction = animation.getAnimatedFraction();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!stateInitialized) {
            return;
        }

        hasDrawn = true;
        int height = getHeight() - topPadding;
        int width = getWidth();
        if (itemHeight == 0) {
            itemHeight = (float)height / pointsCount;
        }

        float currentYRatio;
        float nextYRatio = 0;
        currentYRatio = (float)currentState.maxValue / currentMaxValue;
        if (animating) {
            nextYRatio = (float)nextState.maxValue / currentMaxValue;
        }

        float textPositionDeltaY = lineWidth + axisTextHeight * 0.8f;

        for (int i = 1; i < pointsCount; i++) {
            float alphaScale;
            if (animating) {
                alphaScale = 1 - animatedFraction;
            }
            else {
                alphaScale = 1;
            }

            float y = height - itemHeight * i * currentYRatio - lineWidth + topPadding;
            linePaint.setAlpha((int)(255 * alphaScale));
            canvas.drawLine(0, y, width, y, linePaint);

            textPaint.setAlpha(linePaint.getAlpha());
            canvas.drawText(currentState.points[i],
                    paddingHorizontal, y - textPositionDeltaY, textPaint);

            if (nextYRatio != 0) {
                y = height - itemHeight * i * nextYRatio - lineWidth + topPadding;
                linePaint.setAlpha((int)(255 * (1 - alphaScale)));
                canvas.drawLine(0, y, width, y, linePaint);

                textPaint.setAlpha(linePaint.getAlpha());
                canvas.drawText(nextState.points[i],
                        paddingHorizontal, y - textPositionDeltaY, textPaint);
            }
        }

        int y = height - lineWidth + topPadding;
        linePaint.setAlpha(255);
        canvas.drawLine(0, y, width, y, linePaint);

        textPaint.setAlpha(255);
        canvas.drawText(currentState.points[0], paddingHorizontal,
                height - textPositionDeltaY + topPadding, textPaint);
    }

    private void startAnimation() {
        if (animator != null) {
            animator.cancel();
        }
        animator = ObjectAnimator.ofInt(
                currentMaxValue, nextState.maxValue);
        animator.addUpdateListener(this);
        animator.addListener(this);
        animator.setDuration(200);
        animator.start();
    }

    public void setPointsCount(int count) {
        pointsCount = count;

        currentState = new State(count);
        nextState = new State(count);
    }

    public void setMaxValue(int maxValue) {
        // no need for animation if we just started
        if (currentState.maxValue == -1 || !hasDrawn) {
            currentMaxValue = maxValue;
            calculateState(currentState, maxValue);
            invalidate();
        }
        else if (maxValue != nextState.maxValue && maxValue != currentMaxValue) {
            int delta = Math.abs(maxValue - currentMaxValue);
            float fraction = (float)delta / Math.min(currentMaxValue, maxValue);
            if (fraction < ANIMATION_START_THRESHOLD) {
                // no need for animation if the max values are close
                if (!animating) {
                    currentMaxValue = maxValue;
                    calculateState(currentState, maxValue);
                    invalidate();
                }
                else {
                    pendingMaxValue = maxValue;
                }
            }
            else {
                if (animating) {
                    // if we are already animating in the same direction
                    // and the next max values are not too much different
                    // we just continue the animation and remember the new max value
                    if (maxValue < nextState.maxValue &&
                            currentState.maxValue > nextState.maxValue) {
                        fraction = (float)maxValue / nextState.maxValue;
                        if (fraction > ANIMATION_INTERRUPT_THRESHOLD) {
                            pendingMaxValue = maxValue;
                            return;
                        }
                    }
                    if (maxValue > nextState.maxValue &&
                            currentState.maxValue < nextState.maxValue) {
                        fraction = (float)currentMaxValue / maxValue;
                        if (fraction > ANIMATION_INTERRUPT_THRESHOLD) {
                            pendingMaxValue = maxValue;
                            return;
                        }
                    }
                }
                pendingMaxValue = -1;

                calculateState(nextState, maxValue);

                if (animating || !nextState.equals(currentState)) {
                    startAnimation();
                }
            }
        }
    }

    private void calculateState(State state, int maxValue) {
        stateInitialized = true;
        state.maxValue = maxValue;
        int valueDelta = Math.round((float)maxValue / pointsCount);
        for (int i = 1; i < pointsCount; i++) {
            int value = i * valueDelta;
            String text;

            // todo review formatting
            if (value >= 1000000) {
                float valueK = value / 1000000f;
                text = (String.format(Locale.US, "%.1f", valueK) + "M")
                        .replace(".0M", "M");
            }
            else if (value >= 1000) {
                float valueK = value / 1000f;
                text = (String.format(Locale.US, "%.1f", valueK) + "K")
                        .replace(".0K", "K");
            }
            else {
                text = String.valueOf(value);
            }

            state.points[i] = text;
        }
    }

    private static class State {

        public final String[] points;
        public int maxValue = -1;

        public State(int pointsCount) {
            points = new String[pointsCount];
            points[0] = "0";
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof State)) {
                return false;
            }
            State state = (State)obj;

            if (state.maxValue != maxValue) {
                return false;
            }

            for (int i = 1; i < points.length; i++) {
                if (!state.points[i].equals(points[i])) {
                    return false;
                }
            }
            return true;
        }
    }
}
