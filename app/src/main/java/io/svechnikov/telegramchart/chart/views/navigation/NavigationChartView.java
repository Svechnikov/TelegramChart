package io.svechnikov.telegramchart.chart.views.navigation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.ScrollListener;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import timber.log.Timber;

public class NavigationChartView extends FrameLayout {

    private final NavigationPlotView plotView;
    private boolean boundsInit = false;
    private final float[] bounds = new float[]{0, 0}; // bounds in axis units

    private final int selectionHorizontalBoundWidth;
    private final int selectionVerticalBoundWidth;
    private final int sliderTickWidth;
    private final int sliderTickHeight;
    private final int paddingHorizontal;
    private final int overlayColor;
    private final int overlayBoundColor;
    private final int overlayBoundTickColor;
    private final Paint paint = new Paint();
    private final ScrollListener scrollListener = new ScrollListener();
    private final GestureDetector gestureDetector;
    private final Path path = new Path();

    private int elementsCount = -1;
    private float movingSelectionDelta;
    private NavigationState currentState = NavigationState.IDLE;
    private final List<NavigationStateListener>
            stateListeners = new ArrayList<>();
    private final List<NavigationBoundsListener>
            boundsListeners = new ArrayList<>();
    private int minVisibleItemsCount;
    private float prevCoordX = -1;
    private Bitmap leftSliderBitmap;
    private Bitmap rightSliderBitmap;
    private ChartData chartData;

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

        Resources r = getResources();

        paddingHorizontal = r.getDimensionPixelSize(
                R.dimen.chart_padding_horizontal);

        plotView = new NavigationPlotView(context);

        addView(plotView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        selectionHorizontalBoundWidth = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_overlay_hor_bound_width);
        selectionVerticalBoundWidth = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_overlay_ver_bound_width);
        sliderTickWidth = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_overlay_hor_bound_tick_width);
        sliderTickHeight = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_overlay_hor_bound_tick_height);

        int[] style = {R.attr.chartNavigationOverlayColor,
                R.attr.chartNavigationOverlayBoundColor,
                R.attr.chartNavigationOverlayBoundTickColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        overlayColor = ta.getColor(0, Color.TRANSPARENT);
        overlayBoundColor = ta.getColor(1, Color.TRANSPARENT);
        overlayBoundTickColor = ta.getColor(2, Color.TRANSPARENT);
        ta.recycle();

        gestureDetector = new GestureDetector(context, scrollListener);
    }

    public void setChartData(ChartData chartData) {
        this.chartData = chartData;
        plotView.setChartData(chartData);
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
        if (coordX < paddingHorizontal) {
            coordX = paddingHorizontal;
        }
        int maxRightBound = getWidth() - paddingHorizontal;
        if (coordX > maxRightBound) {
            coordX = maxRightBound;
        }
        // not doing anything if coords not changed
        if (coordX == prevCoordX) {
            return true;
        }
        prevCoordX = coordX;
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                switch (currentState) {
                    case MOVING_SELECTION:
                        moveSelection(coordX);
                        break;
                    case MOVING_LEFT_BOUND:
                        moveLeftBound(coordX);
                        break;
                    case MOVING_RIGHT_BOUND:
                        moveRightBound(coordX);
                        break;
                    case IDLE:
                        return false;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                calculateMovingState(coordX);
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
        return (float)(getWidth() - paddingHorizontal * 2) / elementsCount;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        int width = getWidth() - paddingHorizontal * 2;
        float left = bounds[0] / elementsCount * width + paddingHorizontal;
        float right = bounds[1] / elementsCount * width + paddingHorizontal;

        // overlays
        paint.setColor(overlayColor);
        canvas.drawRect(paddingHorizontal, 0,
                left + selectionHorizontalBoundWidth, getHeight(), paint);
        canvas.drawRect(right - selectionHorizontalBoundWidth,
                0, paddingHorizontal + width,
                getHeight(), paint);

        // slider top/bottom bounds
        paint.setColor(overlayBoundColor);
        float verticalBoundLeft = left + selectionHorizontalBoundWidth;
        float verticalBoundRight = right - selectionHorizontalBoundWidth;
        canvas.drawRect(verticalBoundLeft, 0,
                verticalBoundRight, selectionVerticalBoundWidth, paint);
        canvas.drawRect(verticalBoundLeft,
                getHeight() - selectionVerticalBoundWidth,
                verticalBoundRight, getHeight(), paint);

        if (leftSliderBitmap == null) {
            leftSliderBitmap = createSlider(true);
        }
        canvas.drawBitmap(leftSliderBitmap, left, 0, null);

        if (rightSliderBitmap == null) {
            rightSliderBitmap = createSlider(false);
        }
        canvas.drawBitmap(rightSliderBitmap,
                right - selectionHorizontalBoundWidth, 0, null);
    }

    private Bitmap createSlider(boolean isLeft) {
        int height = getHeight();
        Bitmap bitmap = Bitmap.createBitmap(selectionHorizontalBoundWidth, height,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        float sliderTickTop = (height - sliderTickHeight) / 2f;
        float sliderTickLeft = (selectionHorizontalBoundWidth - sliderTickWidth) / 2f;

        paint.setColor(overlayBoundColor);

        float cornerRadius = selectionHorizontalBoundWidth / 2f;
        if (isLeft) {
            canvas.drawPath(RoundedRect(0,
                    0,
                    selectionHorizontalBoundWidth,
                    height,
                    cornerRadius, cornerRadius, true, false, false, true), paint);
        }
        else {
            canvas.drawPath(RoundedRect(0,
                    0,
                    selectionHorizontalBoundWidth,
                    height,
                    cornerRadius, cornerRadius, false, true, true, false), paint);
        }

        // slider ticks
        paint.setColor(overlayBoundTickColor);
        canvas.drawRect(sliderTickLeft,
                sliderTickTop,
                sliderTickLeft + sliderTickWidth,
                sliderTickTop + sliderTickHeight, paint);

        return bitmap;
    }

    public Path RoundedRect(float left,
                            float top,
                            float right,
                            float bottom,
                            float rx,
                            float ry,
                            boolean tl,
                            boolean tr,
                            boolean br,
                            boolean bl) {
        if (rx < 0) {
            rx = 0;
        }
        if (ry < 0) {
            ry = 0;
        }
        float width = right - left;
        float height = bottom - top;
        if (rx > width / 2) {
            rx = width / 2;
        }
        if (ry > height / 2) {
            ry = height / 2;
        }
        float widthMinusCorners = (width - (2 * rx));
        float heightMinusCorners = (height - (2 * ry));

        path.reset();
        path.moveTo(right, top + ry);
        if (tr) {
            path.rQuadTo(0, -ry, -rx, -ry);//top-right corner
        }
        else {
            path.rLineTo(0, -ry);
            path.rLineTo(-rx,0);
        }

        path.rLineTo(-widthMinusCorners, 0);
        if (tl)
            path.rQuadTo(-rx, 0, -rx, ry); //top-left corner
        else {
            path.rLineTo(-rx, 0);
            path.rLineTo(0,ry);
        }
        path.rLineTo(0, heightMinusCorners);

        if (bl) {
            path.rQuadTo(0, ry, rx, ry);//bottom-left corner
        }
        else {
            path.rLineTo(0, ry);
            path.rLineTo(rx,0);
        }

        path.rLineTo(widthMinusCorners, 0);
        if (br) {
            path.rQuadTo(rx, 0, rx, -ry); //bottom-right corner
        }
        else {
            path.rLineTo(rx,0);
            path.rLineTo(0, -ry);
        }

        path.rLineTo(0, -heightMinusCorners);

        path.close();

        return path;
    }

    private void calculateMovingState(float x) {
        float elementWidth = calculateElementWidth();

        float leftBoundPx = bounds[0] * elementWidth + paddingHorizontal;
        float rightBoundPx = bounds[1] * elementWidth + paddingHorizontal;

        if (leftBoundPx >= x && bounds[0] == 0) {
            setState(NavigationState.MOVING_LEFT_BOUND);
            moveLeftBound(x);
            return;
        }
        if (rightBoundPx <= x && bounds[1] == elementsCount) {
            setState(NavigationState.MOVING_RIGHT_BOUND);
            moveRightBound(x);
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

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
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
        if (coordX < paddingHorizontal) {
            coordX = paddingHorizontal;
        }

        float rightBoundPx = bounds[1] * calculateElementWidth() + paddingHorizontal;

        // no overlapping between bounds
        if (coordX + selectionHorizontalBoundWidth * 3 > rightBoundPx) {
            coordX = rightBoundPx - selectionHorizontalBoundWidth * 3;
        }

        float left = Math.max((coordX - paddingHorizontal -
                selectionHorizontalBoundWidth / 2f) / calculateElementWidth(), 0);

        if (bounds[1] - left < minVisibleItemsCount) {
            left = bounds[1] - minVisibleItemsCount;
        }

        if (bounds[0] != left) {
            bounds[0] = left;

            invalidate();
            notifyBoundsListener();
        }
    }

    private void moveRightBound(float coordX) {
        float maxRightBound = getWidth() - paddingHorizontal;
        if (coordX > maxRightBound) {
            coordX = maxRightBound;
        }

        float leftBoundPx = bounds[0] * calculateElementWidth() + paddingHorizontal;

        // no overlapping between bounds
        if (coordX - selectionHorizontalBoundWidth * 3 < leftBoundPx) {
            coordX = leftBoundPx + selectionHorizontalBoundWidth * 3;
        }

        float right = Math.min((coordX - paddingHorizontal +
                selectionHorizontalBoundWidth / 2f) / calculateElementWidth(), elementsCount);

        if (right - bounds[0] < minVisibleItemsCount) {
            right = bounds[0] + minVisibleItemsCount;
        }

        if (bounds[1] != right) {
            bounds[1] = right;

            invalidate();
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

            invalidate();
            notifyBoundsListener();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
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

        invalidate();

        notifyBoundsListener();
    }
}
