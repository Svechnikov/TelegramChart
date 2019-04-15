package io.svechnikov.telegramchart.chart.data;

import java.util.LinkedHashMap;
import java.util.Map;

public class SelectedPoint {

    public String title;
    public float x = -1;
    public final Map<Entity, Integer> values = new LinkedHashMap<>(2);
    public final Map<Entity, Float> coordsY = new LinkedHashMap<>(2);
    public final Map<Entity, Integer> percentValues = new LinkedHashMap<>(2);

    public void addValue(Entity entity, int value) {
        values.put(entity, value);
    }

    public void addCoordY(Entity entity, float coordY) {
        coordsY.put(entity, coordY);
    }

    public boolean isReady() {
        return x != -1;
    }

    public void reset() {
        this.x = -1;
        title = null;
        values.clear();
        coordsY.clear();
    }
}
