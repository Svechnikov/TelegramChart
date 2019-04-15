package io.svechnikov.telegramchart.chart.data;

public class Entity {

    public final int originalColor;
    public int color;
    public final String title;
    public final int[] values;

    private final int maxValue;
    private final int minValue;

    private boolean isVisible = true;

    public Entity(int color,
                  String title,
                  int[] values) {
        this.originalColor = color;
        this.color = color;
        this.title = title;
        this.values = values;

        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;

        for (int value: values) {
            max = Math.max(value, max);
            min = Math.min(value, min);
        }

        minValue = min;
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

    public int minValue() {
        return minValue;
    }
}
