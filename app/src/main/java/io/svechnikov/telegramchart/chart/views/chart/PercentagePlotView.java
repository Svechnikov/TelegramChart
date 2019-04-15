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

public class PercentagePlotView extends FrameLayout
        implements MainPlotView {

    private NavigationBounds bounds;
    private NavigationState state = NavigationState.IDLE;
    private int selectedPointIndex = -1;
    private boolean showSelectedPointWhenReady;

    private ValueAnimator scaleAnimator;

    private final int topPadding;
    private final int paddingHorizontal;

    private final List<Entity> entities = new ArrayList<>();
    private final Map<Entity, Paint> paints = new HashMap<>();
    private int elementsCount = -1;
    private boolean hasDrawn;

    private boolean animationCanceled;
    private Axis horizontalAxis;
    private Entity animatedEntity;
    private float[][] lines;
    private boolean triggerAnimationOnEntityChange = false;
    private final GestureDetector gestureDetector;

    private Map<Entity, float[]> pointCoordsByEntity = new HashMap<>();
    private final SelectedPoint selectedPoint = new SelectedPoint();
    private final ScrollListener scrollListener = new ScrollListener();
    private SelectedPointViewCallback selectedPointViewCallback;
    private final Paint selectedLinePaint = new Paint();

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
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

    public PercentagePlotView(Context context) {
        this(context, null);
    }

    public PercentagePlotView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public PercentagePlotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        Resources r = context.getResources();

        int[] style = {R.attr.dividerColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        int dividerColor = ta.getColor(0, Color.GRAY);
        ta.recycle();

        int lineWidth = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width);

        topPadding = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_plot_top_padding);

        paddingHorizontal = r.getDimensionPixelSize(R.dimen.chart_padding_horizontal);

        setBackgroundColor(Color.TRANSPARENT);

        gestureDetector = new GestureDetector(context, scrollListener);

        selectedLinePaint.setStyle(Paint.Style.STROKE);
        selectedLinePaint.setColor(dividerColor);
        selectedLinePaint.setStrokeWidth(lineWidth);
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
        float[] coords = pointCoordsByEntity.get(visibleEntity);

        float step = coords[left + 1] - coords[left];
        float delta = coords[left];
        selectedPointIndex = (int)(Math.round((x - delta) / step) + left);

        if (selectedPointIndex < 0 ||
                selectedPointIndex >= horizontalAxis.values.length) {
            selectedPointIndex = -1;
            return;
        }

        if (selectedPoint.x == coords[selectedPointIndex]) {
            return;
        }
        if (coords[selectedPointIndex] < 0) {
            selectedPointIndex++;
        }
        if (coords[selectedPointIndex] > getWidth()) {
            selectedPointIndex--;
        }
        selectedPoint.x = coords[selectedPointIndex];

        showSelectedPoint();
    }

    @SuppressWarnings("all")
    private void showSelectedPoint() {
        int sum = 0;
        for (Entity entity: entities) {
            if (entity.isVisible()) {
                sum += entity.values[selectedPointIndex];
            }
        }
        for (Entity entity: entities) {
            if (entity.isVisible()) {
                int value = entity.values[selectedPointIndex];
                int percentage = (int)Math.round(((float)value / sum * 100));
                selectedPoint.addValue(entity, value);
                selectedPoint.percentValues.put(entity, percentage);
            }
        }
        if (selectedPoint.values.isEmpty()) {
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
            paints.put(entity, paint);

            float[] coords = new float[entity.values.length];
            pointCoordsByEntity.put(entity, coords);
        }
    }

    private void startGraphAnimator() {
        if (scaleAnimator != null) {
            scaleAnimator.cancel();
        }

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
        lines = new float[entities.size()][horizontalAxis.values.length * 4];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // todo move non drawing-related code away from onDraw
        if (triggerAnimationOnEntityChange) {
            triggerAnimationOnEntityChange = false;
            startGraphAnimator();
        }
        else {
            if (state != NavigationState.IDLE &&
                    hasDrawn) {
                startGraphAnimator();
            }
        }

        drawBars(canvas);

        if (showSelectedPointWhenReady) {
            Entity visibleEntity = null;
            for (Entity entity: entities) {
                if (entity.isVisible()) {
                    visibleEntity = entity;
                    break;
                }
            }
            if (pointCoordsByEntity.containsKey(visibleEntity)) {
                float[] coords = pointCoordsByEntity.get(visibleEntity);
                if (coords != null && coords.length > selectedPointIndex) {
                    selectedPoint.x = coords[selectedPointIndex];
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

    @SuppressWarnings("ConstantConditions")
    private void drawBars(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();

        float barWidth = (width - paddingHorizontal * 2) / (bounds.getWidth());
        int offBoundsElements = (int)Math.ceil(paddingHorizontal / barWidth);

        if (lines == null) {
            allocateLines();
        }

        int left = Math.max((int)bounds.left - offBoundsElements - 1, 0);
        int right = Math.min((int)Math.ceil(bounds.right) + offBoundsElements + 1,
                horizontalAxis.size() - 1);

        float deltaLineX1 = width - barWidth * bounds.right - paddingHorizontal;

        for (int i = left; i < right; i++) {
            float yFrom = height;
            int index = (i - left) * 4;

            int sum = 0;

            for (Entity entity: entities) {
                if (entity == animatedEntity) {
                    if (entity.isVisible()) {
                        sum += entity.values[i] * scaleAnimator.getAnimatedFraction();
                    }
                    else {
                        sum += entity.values[i] * (1 - scaleAnimator.getAnimatedFraction());
                    }
                }
                else if (entity.isVisible()) {
                    sum += entity.values[i];
                }
            }

            for (int j = 0; j < entities.size(); j++) {
                Entity entity = entities.get(j);

                int value = entity.values[i];

                if (entity == animatedEntity) {
                    if (entity.isVisible()) {
                        value *= scaleAnimator.getAnimatedFraction();
                    }
                    else {
                        value *= (1 - scaleAnimator.getAnimatedFraction());
                    }
                }
                else if (!entity.isVisible()) {
                    continue;
                }

                float percentage = (float)value / sum;

                float yTo = yFrom - percentage * (height - topPadding);
                float x = i * barWidth + deltaLineX1;

                float[] selectableCoords = pointCoordsByEntity.get(entity);
                selectableCoords[i] = x;

                float[] lines = this.lines[j];
                lines[index] = x;
                lines[index + 1] = yFrom;
                lines[index + 2] = x;
                lines[index + 3] = yTo;

                yFrom = yTo;
            }
        }

        int pointsCount = (right - left) * 4;
        for (int i = 0; i < entities.size(); i++) {
            Entity entity = entities.get(i);
            if (entity.isVisible() || entity == animatedEntity) {
                Paint paint = paints.get(entity);
                paint.setStrokeWidth(barWidth);
                float[] lines = this.lines[i];

                canvas.drawLines(lines, 0, pointsCount, paint);
            }
        }

        if (selectedPointIndex != -1) {
            float x = selectedPointIndex * barWidth + deltaLineX1;

            canvas.drawLine(x, height, x, topPadding, selectedLinePaint);
        }
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

}
