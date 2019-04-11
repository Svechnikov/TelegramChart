package io.svechnikov.telegramchart.chart.views;


import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import io.svechnikov.telegramchart.chart.NavigationStateListener;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.views.horizontal_axis.AnimatedHorizontalAxisView;
import io.svechnikov.telegramchart.chart.views.horizontal_axis.AnimatedHorizontalAxisView;

public class MainChartView extends RelativeLayout
        implements NavigationBoundsListener, NavigationStateListener {

    private final VerticalAxisView verticalAxisView;
    private final MainPlotView plotView;
    private final AnimatedHorizontalAxisView horizontalAxisView;
    private final List<Entity> entities = new ArrayList<>();
    private NavigationBounds currentBounds;

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

        verticalAxisView = createVerticalAxis();
        horizontalAxisView = createHorizontalAxis();
        plotView = createPlotView();
    }

    public int getSelectedPointIndex() {
        return plotView.getSelectedPointIndex();
    }

    public void setSelectedPointIndex(int index) {
        plotView.setSelectedPointIndex(index);
    }

    public void setVerticalItemsCount(int count) {
        verticalAxisView.setPointsCount(count);
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

    private void updateMaxValue() {
        int globalMaxValue = Integer.MIN_VALUE;
        for (Entity entity: entities) {
            if (!entity.isVisible()) {
                continue;
            }
            int maxValue = Integer.MIN_VALUE;
            int left = Math.max((int)currentBounds.left - 1, 0);
            int right = Math.min((int)Math.ceil(currentBounds.right) + 1, entity.values.length);
            for (int i = left; i < right; i++) {
                maxValue = Math.max(maxValue, entity.values[i]);
            }
            globalMaxValue = Math.max(globalMaxValue, maxValue);
        }

        if (globalMaxValue != Integer.MIN_VALUE) {
            verticalAxisView.setMaxValue(globalMaxValue);
            plotView.setMaxValue(globalMaxValue);
        }
    }

    @Override
    public void onNavigationStateChanged(NavigationState state) {
        horizontalAxisView.onNavigationStateChanged(state);
        plotView.onNavigationStateChanged(state);
    }

    public void onEntityChanged(Entity entity) {
        plotView.onEntityChanged(entity);
        updateMaxValue();
    }

    public void setHorizontalAxis(Axis horizontalAxis) {
        horizontalAxisView.setAxis(horizontalAxis);
        plotView.setHorizontalAxis(horizontalAxis);
    }

    public void addEntity(Entity entity) {
        plotView.addEntity(entity);
        entities.add(entity);
    }

    private MainPlotView createPlotView() {
        MainPlotView view = new MainPlotView(getContext());
        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.horizontalAxisView);
        addView(view, lp);
        return view;
    }

    private VerticalAxisView createVerticalAxis() {
        VerticalAxisView view = new VerticalAxisView(getContext());
        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.ABOVE, R.id.horizontalAxisView);
        addView(view, lp);
        return view;
    }

    private AnimatedHorizontalAxisView createHorizontalAxis() {
        Context context = getContext();
        AnimatedHorizontalAxisView view = new AnimatedHorizontalAxisView(context);
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
