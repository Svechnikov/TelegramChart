package io.svechnikov.telegramchart.chart.data;

public class Entity {

    public final int color;
    public final String title;
    public final int[] values;

    private final int maxValue;

    private boolean isVisible = true;

    public Entity(int color, String title, int[] values) {
        this.color = color;
        this.title = title;
        this.values = values;

        int max = 0;

        for (int i = 0; i < values.length; i++) {
            max = Math.max(values[i], max);
        }

        maxValue = max;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public int maxValue() {
        return maxValue;
    }
}
