package io.svechnikov.telegramchart.chart;

import io.svechnikov.telegramchart.chart.data.NavigationState;

public interface NavigationStateListener {
    void onNavigationStateChanged(NavigationState state);
}
