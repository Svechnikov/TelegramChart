package io.svechnikov.telegramchart.chart.views.verticalaxis;

public interface VerticalAxisView {
    void setViewPoints(VerticalAxisViewPoint[] state);
    void invalidate();
}
