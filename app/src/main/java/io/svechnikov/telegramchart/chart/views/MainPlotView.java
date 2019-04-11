package io.svechnikov.telegramchart.chart.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.ScrollListener;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import io.svechnikov.telegramchart.chart.data.SelectedPoint;

public class MainPlotView extends FrameLayout
        implements NavigationBoundsListener, NavigationStateListener {

    private NavigationBounds bounds;
    private NavigationState state = NavigationState.IDLE;
    private int currentMaxValue;
    private int nextMaxValue;
    private int maxValue;
    private int selectedPointIndex = -1;
    private boolean showSelectedPointWhenReady;

    private ValueAnimator scaleAnimator;

    private final int topPadding;
    private final int paddingHorizontal;
    private final int selectedCircleRadius;

    private final Paint topGradientPaint = new Paint();
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
    private final SelectedPointView selectedPointView;
    private final ScrollListener scrollListener = new ScrollListener();

    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    currentMaxValue = (int)animation.getAnimatedValue();
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

    public MainPlotView(Context context) {
        this(context, null);
    }

    public MainPlotView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MainPlotView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        LinearGradient gradient = new LinearGradient(0, 0, 0,
                topPadding, chartBackground,
                getGradientColor(), Shader.TileMode.MIRROR);
        topGradientPaint.setShader(gradient);

        paddingHorizontal = r.getDimensionPixelSize(R.dimen.chart_padding_horizontal);

        selectedCirclePaint.setStyle(Paint.Style.FILL);
        selectedLinePaint.setStyle(Paint.Style.STROKE);
        selectedLinePaint.setStrokeWidth(r.getDimensionPixelSize(
                R.dimen.chart_main_chart_divider_width));
        selectedLinePaint.setColor(dividerColor);

        selectedCircleRadius = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_selected_circle_radius);

        selectedPointView = createSelectedPointView();

        setBackgroundColor(Color.TRANSPARENT);

        gestureDetector = new GestureDetector(context, scrollListener);
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

    public void setSelectedPointIndex(int selectedPointIndex) {
        this.selectedPointIndex = selectedPointIndex;
        if (selectedPointIndex != -1) {
            showSelectedPointWhenReady = true;
        }
    }

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
                horizontalAxis.values[selectedPointIndex].fullName;
        selectedPointView.show(!showSelectedPointWhenReady);
        showSelectedPointWhenReady = false;
        // on some devices there might be blank areas
        // on charts when shown selected points.
        // It has something to do with hw precessing.
        // So we temporary disable hw layer
        setLayerType(LAYER_TYPE_NONE, null);
        invalidate();
    }

    private SelectedPointView createSelectedPointView() {
        SelectedPointView view = new SelectedPointView(getContext());
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setPoint(selectedPoint);
        addView(view, lp);
        return view;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setHorizontalAxis(Axis horizontalAxis) {
        this.horizontalAxis = horizontalAxis;
    }

    // On some devices when using Color.TRANSPARENT (#00000000) gradient gets gray
    // So we calculate background-based transparent color
    private int getGradientColor() {
        int red = Color.red(chartBackground);
        int green = Color.green(chartBackground);
        int blue = Color.blue(chartBackground);

        return Color.argb(0, red, green, blue);
    }

    public void addEntity(Entity entity) {
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

    private void startGraphAnimator(int maxValue) {
        if (scaleAnimator != null) {
            scaleAnimator.cancel();
        }
        nextMaxValue = maxValue;
        scaleAnimator = ObjectAnimator.ofInt(currentMaxValue, maxValue);
        scaleAnimator.addUpdateListener(animatorUpdateListener);
        scaleAnimator.addListener(animatorListener);
        scaleAnimator.setDuration(200);
        scaleAnimator.start();
    }

    public void onEntityChanged(Entity entity) {
        if (hasDrawn) {
            animatedEntity = entity;
            triggerAnimationOnEntityChange = true;
        }
        resetSelectedPoint();
        invalidate();
    }

    private void drawPlots(Canvas canvas) {
        for (Entity entity: entities) {
            if (entity.isVisible() || entity == animatedEntity) {
                drawPlot(canvas, entity);
            }
        }
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
        float scaleY = (float)height / currentMaxValue;
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
            float x = (width + (right - bounds.right) * scaleX - paddingHorizontal);
            float y = (currentMaxValue - values[right]) * scaleY + topPadding;
            selectableCoords[right].update(x, y);

            for (int i = right - 1; i >= left; i--) {
                x = width - paddingHorizontal - (bounds.right - i) * scaleX;
                y = (currentMaxValue - values[i]) * scaleY + topPadding;
                selectableCoords[i].update(x, y);
                pointCoordsByEntity.get(entity)[right].update(x, y);

                lines[(i - left) * 4] = width - (bounds.right - i) * scaleX - paddingHorizontal;
                lines[(i - left) * 4 + 1] = (currentMaxValue - values[i]) * scaleY + topPadding;

                lines[(i - left) * 4 + 2] = width - (bounds.right - i - 1) * scaleX - paddingHorizontal;
                lines[(i - left) * 4 + 3] = (currentMaxValue - values[i + 1]) * scaleY + topPadding;
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
            float deltaLineX2 = width - scaleX * bounds.right + scaleX - paddingHorizontal;

            for (int i = left; i < right; i++) {
                x = scaleX * i + deltaX;
                y = deltaY - scaleY * values[i];
                selectableCoords[i].update(x, y);

                lines[(i - left) * 4] = deltaLineX1 + scaleX * i;
                lines[(i - left) * 4 + 1] = deltaLineY - scaleY * values[i];

                lines[(i - left) * 4 + 2] = deltaLineX2 + scaleX * i;
                lines[(i - left) * 4 + 3] = deltaLineY - scaleY * values[i + 1];
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

        canvas.drawRect(0, 0, width, topPadding, topGradientPaint);

        if (selectedPoint.isReady()) {
            drawSelectedPoint(canvas, entity);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void drawSelectedPoint(Canvas canvas, Entity entity) {
        float x = selectedPoint.x;
        float y = selectedPoint.coordsY.get(entity);

        int circleRadius = selectedCircleRadius;

        selectedCirclePaint.setColor(entity.color);
        canvas.drawCircle(x, y, circleRadius, selectedCirclePaint);

        selectedCirclePaint.setColor(chartBackground);
        canvas.drawCircle(x, y, circleRadius * 0.6f, selectedCirclePaint);
    }

    public void setMaxValue(int value) {
        maxValue = value;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // todo move non drawing-related code away from onDraw
        if (triggerAnimationOnEntityChange) {
            triggerAnimationOnEntityChange = false;
            startGraphAnimator(maxValue);
        }
        else {
            if (scaleAnimator == null &&
                    (currentMaxValue == 0 || state == NavigationState.IDLE)) {
                currentMaxValue = maxValue;
            }
            if (state != NavigationState.IDLE &&
                    maxValue != nextMaxValue && hasDrawn) {
                startGraphAnimator(maxValue);
            }
        }

        if (selectedPoint.isReady()) {
            float x = selectedPoint.x;

            canvas.drawLine(x, 0, x, getHeight(), selectedLinePaint);
        }

        drawPlots(canvas);

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
        selectedPointView.hide();
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
