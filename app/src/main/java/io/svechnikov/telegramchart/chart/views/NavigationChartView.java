package io.svechnikov.telegramchart.chart.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.ScrollListener;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.data.Entity;

public class NavigationChartView extends FrameLayout {

    private final NavigationPlotView plotView;
    private boolean boundsInit = false;
    private final float[] bounds = new float[]{0, 0}; // bounds in axis units

    private final int selectionHorizontalBoundWidth;
    private final int selectionVerticalBoundWidth;
    private final View leftOverlay;
    private final View rightOverlay;
    private final View leftSelectionBound;
    private final View rightSelectionBound;
    private final View topSelectionBound;
    private final View bottomSelectionBound;
    private final ScrollListener scrollListener = new ScrollListener();
    private final GestureDetector gestureDetector;

    private int elementsCount = -1;
    private float movingSelectionDelta;
    private NavigationState currentState = NavigationState.IDLE;
    private final List<NavigationStateListener>
            stateListeners = new ArrayList<>();
    private final List<NavigationBoundsListener>
            boundsListeners = new ArrayList<>();
    private int minVisibleItemsCount;
    private float prevCoordX = -1;

    public NavigationChartView(Context context) {
        this(context, null);
    }

    public NavigationChartView(Context context,
                               @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NavigationChartView(Context context,
                               @Nullable AttributeSet attrs,
                               int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        plotView = new NavigationPlotView(context);

        addView(plotView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Resources r = getResources();

        selectionHorizontalBoundWidth = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_overlay_hor_bound_width);
        selectionVerticalBoundWidth = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_overlay_ver_bound_width);

        int[] style = {R.attr.chartNavigationOverlayColor,
                R.attr.chartNavigationOverlayBoundColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        int overlayColor = ta.getColor(0, Color.TRANSPARENT);
        int overlayBoundColor = ta.getColor(1, Color.TRANSPARENT);
        ta.recycle();

        leftOverlay = createOverlayView(overlayColor);
        rightOverlay = createOverlayView(overlayColor);

        leftSelectionBound = createSelectionHorizontalBoundView(overlayBoundColor);
        rightSelectionBound = createSelectionHorizontalBoundView(overlayBoundColor);

        topSelectionBound = createSelectionVerticalBoundView(overlayBoundColor);
        bottomSelectionBound = createSelectionVerticalBoundView(overlayBoundColor);

        gestureDetector = new GestureDetector(context, scrollListener);
    }

    private View createSelectionVerticalBoundView(int color) {
        View bound = new View(getContext());
        bound.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                selectionVerticalBoundWidth));
        bound.setBackgroundColor(color);
        addView(bound);
        return bound;
    }

    private View createSelectionHorizontalBoundView(int color) {
        View bound = new View(getContext());
        bound.setLayoutParams(new FrameLayout.LayoutParams(
                selectionHorizontalBoundWidth,
                ViewGroup.LayoutParams.MATCH_PARENT));
        bound.setBackgroundColor(color);
        addView(bound);
        return bound;
    }

    private View createOverlayView(int color) {
        View overlay = new View(getContext());
        overlay.setBackgroundColor(color);
        addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return overlay;
    }

    public NavigationBounds getBounds() {
        return new NavigationBounds(bounds[0], bounds[1]);
    }

    public void setHorizontalItemsCount(int count) {
        minVisibleItemsCount = count;
    }

    public void addStateListener(NavigationStateListener listener) {
        stateListeners.add(listener);
    }

    public void addBoundsListener(NavigationBoundsListener listener) {
        boundsListeners.add(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float coordX = event.getX();
        // not doing anything if coords not changed
        if (coordX == prevCoordX) {
            return true;
        }
        prevCoordX = coordX;
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                switch (currentState) {
                    case MOVING_SELECTION:
                        moveSelection(event.getX());
                        break;
                    case MOVING_LEFT_BOUND:
                        moveLeftBound(event.getX());
                        break;
                    case MOVING_RIGHT_BOUND:
                        moveRightBound(event.getX());
                        break;
                    case IDLE:
                        return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                calculateMovingState(event.getX());
                boolean idle = currentState == NavigationState.IDLE;
                getParent().requestDisallowInterceptTouchEvent(!idle);
                break;
        }

        if (event.getAction() == MotionEvent.ACTION_UP ||
                scrollListener.shouldStartScrolling()) {
            scrollListener.scrollingStarted();
            getParent().requestDisallowInterceptTouchEvent(false);
            setState(NavigationState.IDLE);
            return super.onTouchEvent(event);
        }
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private void notifyBoundsListener() {
        float left = bounds[0];
        float right = bounds[1] - 1; // decrement since right bound is inclusive
        NavigationBounds bounds = new NavigationBounds(left, right);
        for (NavigationBoundsListener listener: boundsListeners) {
            listener.onNavigationBoundsChanged(bounds);
        }
    }

    public void onEntityChanged(Entity entity) {
        plotView.onEntityChanged(entity);
    }

    private float calculateElementWidth() {
        return (float)getWidth() / elementsCount;
    }

    private void calculateMovingState(float x) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float elementWidth = calculateElementWidth();

        float leftBoundPx = bounds[0] * elementWidth;
        float rightBoundPx = bounds[1] * elementWidth;

        if (leftBoundPx > x && bounds[0] == 0) {
            setState(NavigationState.MOVING_LEFT_BOUND);
            moveLeftBound(x);
            return;
        }
        if (rightBoundPx < x && bounds[1] == elementsCount) {
            setState(NavigationState.MOVING_LEFT_BOUND);
            moveLeftBound(x);
            return;
        }

        // simple cases dragging bounds
        if (x <= leftBoundPx + selectionHorizontalBoundWidth) {
            setState(NavigationState.MOVING_LEFT_BOUND);
            moveLeftBound(x);
            return;
        }
        if (x >= rightBoundPx - selectionHorizontalBoundWidth) {
            setState(NavigationState.MOVING_RIGHT_BOUND);
            moveRightBound(x);
            return;
        }

        float xdpi = displayMetrics.xdpi;

        // if bounds are close (<= 0.2 inches), we move whole selection
        if ((rightBoundPx - leftBoundPx) / xdpi <= 0.2) {
            setState(NavigationState.MOVING_SELECTION);
            calculateMovingSelectionDelta(x, elementWidth);
            return;
        }

        // if we still want to drag bounds, but missed a bit
        float inchesThreshold = 0.05f;

        boolean canMoveLeftBound = (x - leftBoundPx) / xdpi <= inchesThreshold;
        boolean canMoveRightBound = (rightBoundPx - x) / xdpi <= inchesThreshold;

        if (canMoveLeftBound) {
            setState(NavigationState.MOVING_LEFT_BOUND);
            return;
        }

        if (canMoveRightBound) {
            setState(NavigationState.MOVING_RIGHT_BOUND);
            return;
        }

        // one possible state left
        setState(NavigationState.MOVING_SELECTION);
        calculateMovingSelectionDelta(x, elementWidth);
    }

    private void setState(NavigationState state) {
        if (state == currentState) {
            return;
        }
        currentState = state;
        for (NavigationStateListener listener: stateListeners) {
            listener.onNavigationStateChanged(state);
        }
    }

    private void calculateMovingSelectionDelta(float x, float elementWidth) {
        movingSelectionDelta = x - bounds[0] * elementWidth;
    }

    private void moveLeftBound(float coordX) {
        if (coordX < 0) {
            coordX = 0;
        }

        float rightBoundPx = bounds[1] * calculateElementWidth();

        // no overlapping between bounds
        if (coordX + selectionHorizontalBoundWidth * 3 > rightBoundPx) {
            coordX = rightBoundPx - selectionHorizontalBoundWidth * 3;
        }

        float left = coordX / calculateElementWidth();

        if (bounds[1] - left < minVisibleItemsCount) {
            left = bounds[1] - minVisibleItemsCount;
        }

        if (bounds[0] != left) {
            bounds[0] = left;

            updateSelectionViews();
            notifyBoundsListener();
        }
    }

    private void moveRightBound(float coordX) {
        if (coordX > getWidth()) {
            coordX = getWidth();
        }

        float leftBoundPx = bounds[0] * calculateElementWidth();

        // no overlapping between bounds
        if (coordX - selectionHorizontalBoundWidth * 3 < leftBoundPx) {
            coordX = leftBoundPx + selectionHorizontalBoundWidth * 3;
        }

        float right = coordX / calculateElementWidth();

        if (right - bounds[0] < minVisibleItemsCount) {
            right = bounds[0] + minVisibleItemsCount;
        }

        if (bounds[1] != right) {
            bounds[1] = right;

            updateSelectionViews();
            notifyBoundsListener();
        }
    }

    private void moveSelection(float coordX) {
        if (coordX - movingSelectionDelta < 0) {
            coordX = movingSelectionDelta;
        }

        float elementWidth = calculateElementWidth();
        float width = bounds[1] - bounds[0];

        float left = (coordX - movingSelectionDelta) / elementWidth;
        float right = left + width;

        if (right > elementsCount) {
            right = elementsCount;
            left = right - width;
        }

        if (bounds[0] != left || bounds[1] != right) {
            bounds[0] = left;
            bounds[1] = right;

            updateSelectionViews();
            notifyBoundsListener();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        updateSelectionViews();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // We could have just called invalidate() and draw everything on canvas,
    // but it would require recreation of display lists with performance penalty.
    // Changing translation and scale is almost free compared to redrawing
    private void updateSelectionViews() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (width == 0 || height == 0) {
            return;
        }
        
        leftOverlay.setPivotX(0);
        leftOverlay.setScaleX(bounds[0] / elementsCount);
        leftSelectionBound.setTranslationX(
                leftOverlay.getScaleX() * width);

        rightOverlay.setPivotX(width);
        float rightScale = (bounds[1] / elementsCount);
        rightOverlay.setScaleX(1 - rightScale);
        rightSelectionBound.setTranslationX(
                rightScale * width - selectionHorizontalBoundWidth);

        float verticalBoundScale = (bounds[1] - bounds[0]) / elementsCount -
                (float)selectionHorizontalBoundWidth * 2 / width;
        float verticalBoundTranslation =
                leftSelectionBound.getTranslationX() + selectionHorizontalBoundWidth;
        topSelectionBound.setPivotX(0);
        topSelectionBound.setTranslationX(verticalBoundTranslation);
        topSelectionBound.setScaleX(verticalBoundScale);

        bottomSelectionBound.setPivotX(0);
        bottomSelectionBound.setTranslationX(verticalBoundTranslation);
        bottomSelectionBound.setTranslationY(height - selectionVerticalBoundWidth);
        bottomSelectionBound.setScaleX(verticalBoundScale);
    }

    public void setBounds(NavigationBounds bounds) {
        this.bounds[0] = bounds.left;
        this.bounds[1] = bounds.right;

        notifyBoundsListener();
    }

    public void addEntity(Entity entity) {
        if (elementsCount == -1) {
            elementsCount = entity.values.length;
        }

        if (!boundsInit) {
            boundsInit = true;
            bounds[0] = 0;
            bounds[1] = elementsCount / 2f;
        }
        plotView.addEntity(entity);

        updateSelectionViews();

        notifyBoundsListener();
    }
}
