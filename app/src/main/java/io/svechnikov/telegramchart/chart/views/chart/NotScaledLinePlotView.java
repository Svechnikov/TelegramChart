package io.svechnikov.telegramchart.chart.views.chart;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.ScrollListener;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import io.svechnikov.telegramchart.chart.data.SelectedPoint;
import io.svechnikov.telegramchart.chart.views.SelectedPointViewCallback;

public class NotScaledLinePlotView extends FrameLayout
        implements MainPlotView {

    private NavigationBounds bounds;
    private NavigationState state = NavigationState.IDLE;
    private int selectedPointIndex = -1;
    private boolean showSelectedPointWhenReady;

    private int maxValue;
    private int currentMaxValue;
    private int nextMaxValue;
    private int prevMaxValue;

    private int minValue;
    private int currentMinValue;
    private int nextMinValue;
    private int prevMinValue;

    private ValueAnimator scaleAnimator;

    private final int topPadding;
    private final int paddingHorizontal;
    private final int selectedCircleRadius;

    private final Paint selectedCirclePaint = new Paint();
    private final Paint selectedLinePaint = new Paint();
    private final int chartBackground;

    private final List<Entity> entities = new ArrayList<>();
    private final int strokeWidth;
    private final Map<Entity, Paint> paints = new HashMap<>();
    private int elementsCount = -1;
    private boolean hasDrawn;

    private boolean animationCanceled;
    private Axis horizontalAxis;
    private Entity animatedEntity;
    private float[] lines;
    private boolean triggerAnimationOnEntityChange = false;
    private final GestureDetector gestureDetector;

    private Map<Entity, Coord[]> pointCoordsByEntity = new HashMap<>();
    private final SelectedPoint selectedPoint = new SelectedPoint();
    private final ScrollListener scrollListener = new ScrollListener();
    private SelectedPointViewCallback selectedPointViewCallback;

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = animation.getAnimatedFraction();
                    currentMaxValue = (int)(prevMaxValue +
                            (nextMaxValue - prevMaxValue) * fraction);
                    currentMinValue = (int)(prevMinValue +
                            (nextMinValue - prevMinValue) * fraction);
                    invalidate();
                }
            };
    private final ValueAnimator.AnimatorListener animatorListener =
            new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    animationCanceled = false;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    animatedEntity = null;
                    if (!animationCanceled) {
                        scaleAnimator = null;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    animationCanceled = true;
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            };

    public NotScaledLinePlotView(Context context) {
        this(context, null);
    }

    public NotScaledLinePlotView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NotScaledLinePlotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        Resources r = context.getResources();
        topPadding = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_plot_top_padding);
        strokeWidth = getResources().getDimensionPixelSize(
                R.dimen.chart_main_stroke_width);

        int[] style = {
                R.attr.chartBackground,
                R.attr.dividerColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        chartBackground = ta.getColor(0, Color.WHITE);
        int dividerColor = ta.getColor(1, Color.GRAY);
        ta.recycle();

        paddingHorizontal = r.getDimensionPixelSize(R.dimen.chart_padding_horizontal);

        selectedCirclePaint.setStyle(Paint.Style.FILL);
        selectedLinePaint.setStyle(Paint.Style.STROKE);
        selectedLinePaint.setStrokeWidth(r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width));
        selectedLinePaint.setColor(dividerColor);

        selectedCircleRadius = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_selected_circle_radius);

        setBackgroundColor(Color.TRANSPARENT);

        gestureDetector = new GestureDetector(context, scrollListener);
    }

    @Override
    public void setSelectedPointViewCallback(SelectedPointViewCallback callback) {
        selectedPointViewCallback = callback;

        callback.setSelectedPoint(selectedPoint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP ||
                scrollListener.shouldStartScrolling()) {
            scrollListener.scrollingStarted();
            getParent().requestDisallowInterceptTouchEvent(false);
            return super.onTouchEvent(event);
        }
        getParent().requestDisallowInterceptTouchEvent(true);

        findSelectedPoint(event.getX());
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void setSelectedPointIndex(int selectedPointIndex) {
        this.selectedPointIndex = selectedPointIndex;
        if (selectedPointIndex != -1) {
            showSelectedPointWhenReady = true;
        }
    }

    @Override
    public int getSelectedPointIndex() {
        return selectedPointIndex;
    }

    @SuppressWarnings("all")
    private void findSelectedPoint(float x) {
        int left = (int)bounds.left;
        float closestDistance = Float.MAX_VALUE;
        Entity visibleEntity = null;
        for (Entity entity: entities) {
            if (entity.isVisible()) {
                visibleEntity = entity;
                break;
            }
        }
        Coord[] coords = pointCoordsByEntity.get(visibleEntity);

        float step = coords[left + 1].x - coords[left].x;
        float delta = coords[left].x;
        selectedPointIndex = (int)(Math.round((x - delta) / step) + left);

        if (selectedPointIndex < 0 ||
                selectedPointIndex >= horizontalAxis.values.length) {
            selectedPointIndex = -1;
            return;
        }

        if (selectedPoint.x == coords[selectedPointIndex].x) {
            return;
        }
        if (coords[selectedPointIndex].x < 0) {
            selectedPointIndex++;
        }
        if (coords[selectedPointIndex].x > getWidth()) {
            selectedPointIndex--;
        }
        selectedPoint.x = coords[selectedPointIndex].x;

        showSelectedPoint();
    }

    @SuppressWarnings("all")
    private void showSelectedPoint() {
        for (Entity entity: entities) {
            if (entity.isVisible()) {
                selectedPoint.addCoordY(entity,
                        pointCoordsByEntity.get(entity)[selectedPointIndex].y);
                selectedPoint.addValue(entity,
                        entity.values[selectedPointIndex]);
            }
        }
        if (selectedPoint.coordsY.isEmpty()) {
            showSelectedPointWhenReady = false;
            resetSelectedPoint();
            return;
        }
        selectedPoint.title =
                horizontalAxis.values[selectedPointIndex].selectedName;
        selectedPointViewCallback.show(!showSelectedPointWhenReady);
        showSelectedPointWhenReady = false;
        // on some devices there might be blank areas
        // on charts when shown selected points.
        // It has something to do with hw processing.
        // So we temporarily disable hw layer
        setLayerType(LAYER_TYPE_NONE, null);
        invalidate();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void setHorizontalAxis(Axis horizontalAxis) {
        this.horizontalAxis = horizontalAxis;
    }

    @Override
    public void setChartData(ChartData chartData) {
        for (Entity entity: chartData.entities) {
            if (elementsCount == -1) {
                elementsCount = entity.values.length;
            }
            else if (elementsCount != entity.values.length) {
                throw new IllegalArgumentException();
            }

            entities.add(entity);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(entity.color);
            paint.setStrokeWidth(strokeWidth);
            paint.setAntiAlias(true);
            paints.put(entity, paint);

            Coord[] coords = new Coord[entity.values.length];
            for (int i = 0; i < coords.length; i++) {
                coords[i] = new Coord();
            }
            pointCoordsByEntity.put(entity, coords);
        }
    }

    private void startGraphAnimator(int maxValue, int minValue) {
        if (scaleAnimator != null) {
            scaleAnimator.cancel();
        }

        prevMaxValue = currentMaxValue;
        nextMaxValue = maxValue;

        prevMinValue = currentMinValue;
        nextMinValue = minValue;

        scaleAnimator = ObjectAnimator.ofInt(0, 1);
        scaleAnimator.addUpdateListener(animatorUpdateListener);
        scaleAnimator.addListener(animatorListener);
        scaleAnimator.setDuration(200);
        scaleAnimator.start();
    }

    @Override
    public void onEntityChanged(Entity entity) {
        if (hasDrawn) {
            animatedEntity = entity;
            triggerAnimationOnEntityChange = true;
        }
        resetSelectedPoint();
        invalidate();
    }

    private void allocateLines() {
        lines = new float[horizontalAxis.values.length * 4];
    }

    @SuppressWarnings("all")
    private void drawPlot(Canvas canvas, Entity entity) {
        int width = getWidth();
        int height = getHeight() - topPadding;
        int[] values = entity.values;
        float scaleX = (width - paddingHorizontal * 2) / (bounds.getWidth() - 1);
        float scaleY = (float)height / (currentMaxValue - currentMinValue);
        int offBoundsElements = (int)Math.ceil(paddingHorizontal / scaleX);

        if (lines == null) {
            allocateLines();
        }
        int left = Math.max((int)bounds.left - offBoundsElements, 0);
        int right = Math.min((int)Math.ceil(bounds.right) + offBoundsElements,
                entity.values.length - 1);

        Coord[] selectableCoords = pointCoordsByEntity.get(entity);

        // draw right to left when moving left bound to get rid of flickering
        if (state == NavigationState.MOVING_LEFT_BOUND) {
            float x = (width + (right - bounds.right) *
                    scaleX - paddingHorizontal);
            float y = (currentMaxValue - values[right]) *
                    scaleY + topPadding;
            selectableCoords[right].update(x, y);

            for (int i = right - 1; i >= left; i--) {
                x = width - paddingHorizontal - (bounds.right - i) * scaleX;
                y = (currentMaxValue - values[i]) * scaleY + topPadding;
                selectableCoords[i].update(x, y);
                pointCoordsByEntity.get(entity)[right].update(x, y);

                int index = (i - left) * 4;

                lines[index] = width - (bounds.right - i) *
                        scaleX - paddingHorizontal;
                lines[index + 1] = (currentMaxValue - values[i]) *
                        scaleY + topPadding;

                lines[index + 2] = width - (bounds.right - i - 1) *
                        scaleX - paddingHorizontal;
                lines[index + 3] = (currentMaxValue - values[i + 1]) *
                        scaleY + topPadding;
            }
        }
        else {
            float x = (left - bounds.left) * scaleX + paddingHorizontal;
            float y = (currentMaxValue - values[left]) * scaleY + topPadding;
            selectableCoords[left].update(x, y);

            float deltaX = paddingHorizontal - scaleX * bounds.left;
            float deltaY = topPadding + scaleY * currentMaxValue;
            float deltaLineY = scaleY * currentMaxValue + topPadding;
            float deltaLineX1 = width - scaleX * bounds.right - paddingHorizontal;
            float deltaLineX2 = width - scaleX * bounds.right +
                    scaleX - paddingHorizontal;

            for (int i = left; i < right; i++) {
                x = scaleX * i + deltaX;
                y = deltaY - scaleY * values[i];
                selectableCoords[i].update(x, y);

                int index = (i - left) * 4;
                lines[index] = deltaLineX1 + scaleX * i;
                lines[index + 1] = deltaLineY - scaleY * values[i];

                lines[index + 2] = deltaLineX2 + scaleX * i;
                lines[index + 3] = deltaLineY - scaleY * values[i + 1];
            }
        }

        int alpha = 255;
        if (entity == animatedEntity && scaleAnimator != null) {
            alpha = (int)(scaleAnimator.getAnimatedFraction() * 255);
            if (!entity.isVisible()) {
                alpha = 255 - alpha;
            }
        }
        Paint paint = paints.get(entity);
        if (paint.getAlpha() != alpha) {
            paint.setAlpha(alpha);
        }
        int pointsCount = (right - left) * 4;
        canvas.drawLines(lines, 0, pointsCount, paint);
        selectableCoords[right].update(lines[pointsCount - 2], lines[pointsCount - 1]);

        if (selectedPoint.isReady()) {
            drawSelectedPoint(canvas, entity);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void drawSelectedPoint(Canvas canvas, Entity entity) {
        float x = selectedPoint.x;
        float y = selectedPoint.coordsY.get(entity);

        int height = getHeight();
        if (y + selectedCircleRadius > height) {
            y = height - selectedCircleRadius;
        }

        selectedCirclePaint.setColor(entity.color);
        canvas.drawCircle(x, y, selectedCircleRadius, selectedCirclePaint);

        selectedCirclePaint.setColor(chartBackground);
        canvas.drawCircle(x, y, selectedCircleRadius * 0.6f, selectedCirclePaint);
    }

    public void updateValues(int min, int max) {
        minValue = min;
        maxValue = max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // todo move non drawing-related code away from onDraw
        if (triggerAnimationOnEntityChange) {
            triggerAnimationOnEntityChange = false;
            startGraphAnimator(maxValue, minValue);
        }
        else {
            if (scaleAnimator == null &&
                    (currentMaxValue == 0 || state == NavigationState.IDLE)) {
                currentMaxValue = maxValue;
                currentMinValue = minValue;
            }
            if (state != NavigationState.IDLE &&
                    (maxValue != nextMaxValue || minValue != nextMinValue) &&
                    hasDrawn) {
                startGraphAnimator(maxValue, minValue);
            }
        }

        if (selectedPoint.isReady()) {
            float x = selectedPoint.x;

            canvas.drawLine(x, 0, x, getHeight(), selectedLinePaint);
        }

        for (Entity entity: entities) {
            if (entity.isVisible() || entity == animatedEntity) {
                drawPlot(canvas, entity);
            }
        }

        if (showSelectedPointWhenReady) {
            Entity visibleEntity = null;
            for (Entity entity: entities) {
                if (entity.isVisible()) {
                    visibleEntity = entity;
                    break;
                }
            }
            if (pointCoordsByEntity.containsKey(visibleEntity)) {
                Coord[] coords = pointCoordsByEntity.get(visibleEntity);
                if (coords != null && coords.length > selectedPointIndex) {
                    selectedPoint.x = coords[selectedPointIndex].x;
                    showSelectedPoint();
                }
            }
            else {
                showSelectedPointWhenReady = false;
            }
        }

        hasDrawn = true;
        super.onDraw(canvas);
    }

    @Override
    public void onNavigationBoundsChanged(NavigationBounds bounds) {
        this.bounds = bounds;
        resetSelectedPoint();
        postInvalidate();
    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        this.state = state;
        resetSelectedPoint();
    }

    private void resetSelectedPoint() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        selectedPoint.reset();
        selectedPointViewCallback.hide();
        selectedPointIndex = -1;
    }

    private static class Coord {
        public float x;
        public float y;

        public void update(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

}
