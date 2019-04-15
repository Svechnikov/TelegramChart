package io.svechnikov.telegramchart.chart.views.chart;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import io.svechnikov.telegramchart.chart.data.SelectedPoint;
import io.svechnikov.telegramchart.chart.views.HorizontalAxisView;
import io.svechnikov.telegramchart.chart.views.SelectedPointView;
import io.svechnikov.telegramchart.chart.views.SelectedPointViewCallback;
import io.svechnikov.telegramchart.chart.views.verticalaxis.VerticalAxisCoordinator;
import io.svechnikov.telegramchart.chart.views.verticalaxis.VerticalAxisLabelsView;
import io.svechnikov.telegramchart.chart.views.verticalaxis.VerticalAxisLinesView;

public class MainChartView extends RelativeLayout
        implements NavigationBoundsListener, NavigationStateListener {

    private VerticalAxisCoordinator verticalAxisCoordinator;
    private MainPlotView plotView;
    private final HorizontalAxisView horizontalAxisView;
    private ChartData chartData;
    private NavigationBounds currentBounds;
    private int verticalItemsCount = -1;
    private final Map<Entity, Integer> minValuesByEntity = new HashMap<>();
    private final Map<Entity, Integer> maxValuesByEntity = new HashMap<>();
    private final Paint topGradientPaint = new Paint();
    private final int chartBackground;
    private final int topPadding;
    private VerticalAxisLabelsView firstVerticalLabelsView;
    private VerticalAxisLabelsView secondVerticalLabelsView;
    private boolean activeFirstVerticalAxis;
    private SelectedPointView selectedPointView;

    public MainChartView(Context context) {
        this(context, null);
    }

    public MainChartView(Context context,
                         AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MainChartView(Context context,
                         AttributeSet attrs,
                         int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        horizontalAxisView = createHorizontalAxis();

        Resources r = getResources();
        topPadding = r.getDimensionPixelSize(
                R.dimen.chart_main_chart_plot_top_padding);

        int[] style = {R.attr.chartBackground};
        TypedArray ta = context.obtainStyledAttributes(style);
        chartBackground = ta.getColor(0, Color.WHITE);
        ta.recycle();

        LinearGradient gradient = new LinearGradient(0, 0, 0,
                topPadding, chartBackground,
                getGradientColor(), Shader.TileMode.MIRROR);
        topGradientPaint.setShader(gradient);
    }

    private SelectedPointView createSelectedPointView() {
        SelectedPointView view = new SelectedPointView(getContext());
        view.setHasPercentValues(chartData.type == ChartData.TYPE_PERCENTAGE);
        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(view, lp);
        return view;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        canvas.drawRect(0, 0, getWidth(), topPadding, topGradientPaint);
    }

    // On some devices when using Color.TRANSPARENT (#00000000) gradient gets gray
    // So we calculate background-based transparent color
    private int getGradientColor() {
        int red = Color.red(chartBackground);
        int green = Color.green(chartBackground);
        int blue = Color.blue(chartBackground);

        return Color.argb(0, red, green, blue);
    }

    public int getSelectedPointIndex() {
        return plotView.getSelectedPointIndex();
    }

    public void setSelectedPointIndex(int index) {
        plotView.setSelectedPointIndex(index);
    }

    public void setVerticalItemsCount(int count) {
        verticalItemsCount = count;
    }

    public void setHorizontalItemsCount(int count) {
        horizontalAxisView.setMinVisibleItemsCount(count);
    }

    @Override
    public void onNavigationBoundsChanged(NavigationBounds bounds) {
        currentBounds = bounds;

        horizontalAxisView.onNavigationBoundsChanged(bounds);
        plotView.onNavigationBoundsChanged(bounds);

        updateMaxValue();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void updateLineMaxValue() {
        int globalMaxValue = Integer.MIN_VALUE;
        int globalMinValue = Integer.MAX_VALUE;

        int left = Math.max((int)currentBounds.left - 1, 0);
        int right = Math.min((int)Math.ceil(currentBounds.right) + 1,
                chartData.axis.size());

        for (Entity entity: chartData.entities) {
            if (!entity.isVisible()) {
                continue;
            }
            int maxValue = Integer.MIN_VALUE;
            int minValue = Integer.MAX_VALUE;
            for (int i = left; i < right; i++) {
                int value = entity.values[i];
                maxValue = Math.max(maxValue, value);
                minValue = Math.min(minValue, value);
            }

            globalMaxValue = Math.max(globalMaxValue, maxValue);
            globalMinValue = Math.min(globalMinValue, minValue);

            if (chartData.type == ChartData.TYPE_Y_SCALED) {
                ((ScaledLinePlotView)plotView)
                        .updateValues(entity, minValue, maxValue);
            }

            maxValuesByEntity.put(entity, maxValue);
            minValuesByEntity.put(entity, minValue);
        }

        if (chartData.type != ChartData.TYPE_Y_SCALED) {
            if (globalMaxValue != Integer.MIN_VALUE) {
                verticalAxisCoordinator
                        .updateValues(globalMinValue, globalMaxValue);
                ((NotScaledLinePlotView)plotView)
                        .updateValues(globalMinValue, globalMaxValue);
            }
        }
        else if (!chartData.entities.isEmpty()) {
            List<Entity> entities = chartData.entities;
            Entity first = entities.get(0);
            Entity second = entities.get(1);

            Integer firstMin = minValuesByEntity.get(first);
            Integer firstMax = maxValuesByEntity.get(first);

            Integer secondMin = minValuesByEntity.get(second);
            Integer secondMax = maxValuesByEntity.get(second);

            if (first.isVisible() && firstMin != null && firstMax != null) {
                verticalAxisCoordinator.updateValues(
                        firstMin, firstMax, activeFirstVerticalAxis);
                activeFirstVerticalAxis = true;

                if (second.isVisible()) {
                    secondVerticalLabelsView.forceValues(secondMin, secondMax);
                }
                else {
                    secondVerticalLabelsView.forceValues(-1, -1);
                }
            }
            else if (second.isVisible() && secondMin != null && secondMax != null) {
                secondVerticalLabelsView.forceValues(-1, -1);
                verticalAxisCoordinator.updateValues(secondMin,
                        secondMax, !activeFirstVerticalAxis);
                activeFirstVerticalAxis = false;
            }
        }
    }

    private void updateStackedMaxValue() {
        int maxValue = 0;

        int left = Math.max((int)currentBounds.left - 1, 0);
        int right = Math.min((int)Math.ceil(currentBounds.right) + 1,
                chartData.axis.size());

        for (int i = left; i < right; i++) {
            int value = 0;
            for (Entity entity: chartData.entities) {
                if (!entity.isVisible()) {
                    continue;
                }
                value += entity.values[i];
            }
            maxValue = Math.max(value, maxValue);
        }

        verticalAxisCoordinator.updateValues(-1, maxValue);

        ((StackedPlotView)plotView).updateMaxValue(maxValue);
    }

    private void updateMaxValue() {
        switch (chartData.type) {
            case ChartData.TYPE_LINE:
            case ChartData.TYPE_Y_SCALED:
                updateLineMaxValue();
                break;
            case ChartData.TYPE_STACKED:
            case ChartData.TYPE_BAR:
                updateStackedMaxValue();
                break;
            case ChartData.TYPE_PERCENTAGE:
                verticalAxisCoordinator.updateValues(0, 100);
                break;
        }
    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        horizontalAxisView.onNavigationStateChanged(state);
        plotView.onNavigationStateChanged(state);

        verticalAxisCoordinator.onNavigationStateChanged(state);
    }

    public void onEntityChanged(Entity entity) {
        plotView.onEntityChanged(entity);
        updateMaxValue();

        if (chartData.type == ChartData.TYPE_Y_SCALED) {
            boolean firstVisible = chartData.entities.get(0).isVisible();
            boolean secondVisible = chartData.entities.get(1).isVisible();
            firstVerticalLabelsView.setVisibility(firstVisible ? View.VISIBLE : View.GONE);
            secondVerticalLabelsView.setVisibility(secondVisible ? View.VISIBLE : View.GONE);
        }
    }

    public void setChartData(ChartData chartData) {
        this.chartData = chartData;

        plotView = createPlotView();

        plotView.setChartData(chartData);

        createVerticalAxis();

        horizontalAxisView.setAxis(chartData.axis);
        plotView.setHorizontalAxis(chartData.axis);

        selectedPointView = createSelectedPointView();
        plotView.setSelectedPointViewCallback(new SelectedPointViewCallback() {
            @Override
            public void setSelectedPoint(SelectedPoint selectedPoint) {
                selectedPointView.setPoint(selectedPoint);
            }

            @Override
            public void show(boolean animate) {
                selectedPointView.show(animate);
            }

            @Override
            public void hide() {
                selectedPointView.hide();
            }
        });

    }

    private MainPlotView createPlotView() {
        View plot;
        switch (chartData.type) {
            case ChartData.TYPE_LINE:
                plot = new NotScaledLinePlotView(getContext());
                break;
            case ChartData.TYPE_Y_SCALED:
                plot = new ScaledLinePlotView(getContext());
                break;
            case ChartData.TYPE_STACKED:
            case ChartData.TYPE_BAR:
                plot = new StackedPlotView(getContext());
                break;
            case ChartData.TYPE_PERCENTAGE:
                plot = new PercentagePlotView(getContext());
                break;
            default:
                plot = new NotScaledLinePlotView(getContext());
        }

        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.horizontalAxisView);
        addView(plot, lp);
        return (MainPlotView) plot;
    }

    private void createVerticalAxis() {
        int type = chartData.type;
        boolean startFromZero = type != ChartData.TYPE_LINE &&
                type != ChartData.TYPE_Y_SCALED;

        verticalAxisCoordinator = new VerticalAxisCoordinator(
                getContext(), verticalItemsCount, startFromZero);

        firstVerticalLabelsView = new VerticalAxisLabelsView(getContext());
        VerticalAxisLinesView linesView = new VerticalAxisLinesView(getContext());
        verticalAxisCoordinator.addView(firstVerticalLabelsView);
        verticalAxisCoordinator.addView(linesView);

        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.horizontalAxisView);

        addView(firstVerticalLabelsView, lp);

        switch (chartData.type) {
            case ChartData.TYPE_LINE:
            case ChartData.TYPE_Y_SCALED:
                addView(linesView, 0, lp);
                break;
            default:
                addView(linesView, lp);
        }

        if (type == ChartData.TYPE_Y_SCALED) {
            firstVerticalLabelsView.setColor(chartData.entities.get(0).color);
            firstVerticalLabelsView.setLabelsBackgroundEnabled(true);

            secondVerticalLabelsView = new VerticalAxisLabelsView(getContext());
            secondVerticalLabelsView.setLabelsBackgroundEnabled(true);
            verticalAxisCoordinator.addView(secondVerticalLabelsView);
            secondVerticalLabelsView.setAlignRight(true);
            secondVerticalLabelsView.setColor(chartData.entities.get(1).color);
            addView(secondVerticalLabelsView, lp);
        }
    }

    private HorizontalAxisView createHorizontalAxis() {
        Context context = getContext();
        HorizontalAxisView view = new HorizontalAxisView(context);
        view.setId(R.id.horizontalAxisView);
        int height = context.getResources()
                .getDimensionPixelSize(
                        R.dimen.chart_main_chart_horizontal_axis_height);

        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);

        addView(view, lp);

        return view;
    }
}
