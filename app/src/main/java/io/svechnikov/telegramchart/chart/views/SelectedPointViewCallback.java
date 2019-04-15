package io.svechnikov.telegramchart.chart.views;

import io.svechnikov.telegramchart.chart.data.SelectedPoint;

public interface SelectedPointViewCallback {
    void setSelectedPoint(SelectedPoint selectedPoint);
    void show(boolean animate);
    void hide();
}
