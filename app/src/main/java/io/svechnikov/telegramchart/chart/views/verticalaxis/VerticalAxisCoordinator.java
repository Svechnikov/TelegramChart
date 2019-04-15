package io.svechnikov.telegramchart.chart.views.verticalaxis;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.data.NavigationState;

// We cannot have one view with both labels and axis-lines
// because lines must be below graph and labels - above
// so we manipulate these 2 views from here
public class VerticalAxisCoordinator implements ValueAnimator.AnimatorUpdateListener,
        Animator.AnimatorListener, NavigationStateListener {

    private static final float MAX_LABEL_BACKGROUND_ALPHA = 0.8f;

    private VerticalAxisViewPoint[] viewPoints;

    private int pointsCount;
    private int height;

    private VerticalAxisState currentState;
    private VerticalAxisState nextState;
    private boolean animationCanceled;
    private boolean hasStarted;

    private int currentMaxValue;
    private float animatedFraction;

    private ValueAnimator animator;
    private boolean animating;
    private int pendingMaxValue = -1;
    private int minValue;
    private boolean startFromZero;
    private float itemHeight;

    private final int topPadding;
    private final int lineWidth;
    private final List<VerticalAxisView> views = new ArrayList<>();

    private static final float ANIMATION_START_THRESHOLD = 0.1f;
    private static final float ANIMATION_INTERRUPT_THRESHOLD = 0.8f;

    public VerticalAxisCoordinator(Context context,
                                   int pointsCount,
                                   boolean startFromZero) {
        Resources r = context.getResources();

        topPadding = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_plot_top_padding);

        lineWidth = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width);

        this.pointsCount = pointsCount;

        viewPoints = new VerticalAxisViewPoint[pointsCount * 2];

        for (int i = 0; i < viewPoints.length; i++) {
            viewPoints[i] = new VerticalAxisViewPoint();
        }

        currentState = new VerticalAxisState(pointsCount);
        nextState = new VerticalAxisState(pointsCount);

        this.startFromZero = startFromZero;
    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        if (state == NavigationState.IDLE) {
            invalidate();
        }
        else {
            invalidate();
        }
    }

    public void addView(final VerticalAxisView axisView) {
        views.add(axisView);
        axisView.setViewPoints(viewPoints);

        ((View)axisView).getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (height != 0) {
                            return;
                        }
                        height = ((View)axisView).getHeight();
                        itemHeight = (float)height / pointsCount;

                        invalidate();
                    }
                });
    }

    private void invalidate() {
        if (currentState.maxValue == 0 || itemHeight == 0 || views.isEmpty()) {
            return;
        }

        int height = this.height - topPadding;

        float currentYRatio;
        float nextYRatio = 0;
        currentYRatio = (float)currentState.maxValue / currentMaxValue;
        if (animating) {
            nextYRatio = (float)nextState.maxValue / currentMaxValue;
        }

        int stateIndex = 0;
        viewPoints[stateIndex++].update(
                height - lineWidth + topPadding, 1,
                currentState.points[0], 1);
        for (int i = 1; i < pointsCount; i++) {
            float alphaScale;
            if (animating) {
                alphaScale = 1 - animatedFraction;
            }
            else {
                alphaScale = 1;
            }

            float y = height - itemHeight * i * currentYRatio - lineWidth + topPadding;

            viewPoints[stateIndex++].update(y, alphaScale,
                    currentState.points[i], Math.min(alphaScale, MAX_LABEL_BACKGROUND_ALPHA));

            if (nextYRatio != 0) {
                y = height - itemHeight * i * nextYRatio - lineWidth + topPadding;
                alphaScale = 1 - alphaScale;

                viewPoints[stateIndex++].update(y, alphaScale,
                        nextState.points[i], Math.min(alphaScale, MAX_LABEL_BACKGROUND_ALPHA));
            }
        }

        for (int i = stateIndex; i < viewPoints.length; i++) {
            viewPoints[i].update(0, 0, null, 0);
        }

        for (VerticalAxisView view: views) {
            view.invalidate();
        }
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

    @Override
    public void onAnimationStart(Animator animation) {
        animating = true;
    }

    private void copyState(VerticalAxisState state1, VerticalAxisState state2) {
        state2.maxValue = state1.maxValue;
        System.arraycopy(state1.points, 0,
                state2.points, 0, pointsCount);

        state1.maxValue = -1;
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        animating = false;
        if (!animationCanceled) {
            copyState(nextState, currentState);
            if (pendingMaxValue != -1) {
                updateValues(minValue, pendingMaxValue);
            }
            invalidate();
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

    public void updateValues(int minValue, int maxValue) {
        updateValues(minValue, maxValue, height != 0);
    }

    public void updateValues(int minValue, int maxValue, boolean animate) {
        if (startFromZero) {
            minValue = 0;
        }
        this.minValue = minValue;
        // no need for animation if we just started
        if (currentState.maxValue == -1 || !hasStarted || !animate) {
            hasStarted = true;
            currentMaxValue = maxValue;
            currentState.updateState(minValue, maxValue);
            invalidate();
        }
        else if (maxValue != nextState.maxValue && maxValue != currentMaxValue) {
            int delta = Math.abs(maxValue - currentMaxValue);
            float fraction = (float)delta / Math.min(currentMaxValue, maxValue);
            if (fraction < ANIMATION_START_THRESHOLD) {
                // no need for animation if the max values are close
                if (!animating) {
                    currentMaxValue = maxValue;
                    currentState.updateState(minValue, maxValue);
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

                nextState.updateState(minValue, maxValue);

                if (animating || !nextState.equals(currentState)) {
                    startAnimation();
                }
            }
        }
    }
}
