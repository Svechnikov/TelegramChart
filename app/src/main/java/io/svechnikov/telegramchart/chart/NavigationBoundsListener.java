package io.svechnikov.telegramchart.chart;

import io.svechnikov.telegramchart.chart.data.NavigationBounds;

public interface NavigationBoundsListener {
    void onNavigationBoundsChanged(NavigationBounds bounds);
}
