package io.svechnikov.telegramchart.chart.views.verticalaxis;

import java.util.Locale;

import androidx.annotation.Nullable;

public class VerticalAxisState {

    public final String[] points;
    public int maxValue;
    public int minValue;

    public VerticalAxisState(int pointsCount) {
        points = new String[pointsCount];
    }

    public void updateState(int minValue, int maxValue) {
        int pointsCount = points.length;
        this.maxValue = maxValue;
        this.minValue = minValue;
        int valueDelta = Math.round((float)(maxValue - minValue) / pointsCount);
        for (int i = 0; i < pointsCount; i++) {
            int value = i * valueDelta + minValue;
            String text;

            // todo review formatting
            if (value >= 1000000) {
                float valueK = value / 1000000f;
                text = (String.format(Locale.US, "%.1f", valueK) + "M")
                        .replace(".0M", "M");
            }
            else if (value >= 1000) {
                float valueK = value / 1000f;
                text = (String.format(Locale.US, "%.1f", valueK) + "K")
                        .replace(".0K", "K");
            }
            else {
                text = String.valueOf(value);
            }

            points[i] = text;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof VerticalAxisState)) {
            return false;
        }
        VerticalAxisState state = (VerticalAxisState)obj;

        if (state.maxValue != maxValue || state.minValue != minValue) {
            return false;
        }

        for (int i = 0; i < points.length; i++) {
            if (!state.points[i].equals(points[i])) {
                return false;
            }
        }
        return true;
    }
}
