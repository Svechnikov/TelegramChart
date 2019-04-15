package io.svechnikov.telegramchart.chart.views.navigation;

import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;

public interface NavigationPlotView {
    void setChartData(ChartData chartData);
    void onEntityChanged(Entity entity);
}
