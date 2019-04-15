package io.svechnikov.telegramchart.chart.views.chart;

import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.data.NavigationState;
import io.svechnikov.telegramchart.chart.views.SelectedPointViewCallback;

public interface MainPlotView {
    int getSelectedPointIndex();
    void setSelectedPointIndex(int index);
    void onNavigationBoundsChanged(NavigationBounds bounds);
    void onNavigationStateChanged(NavigationState state);
    void onEntityChanged(Entity entity);
    void setChartData(ChartData chartData);
    void setHorizontalAxis(Axis axis);
    void setSelectedPointViewCallback(SelectedPointViewCallback callback);
}
