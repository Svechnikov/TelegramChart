package io.svechnikov.telegramchart.chart.data;

import java.util.HashMap;
import java.util.Map;

public class SelectablePoint {

    private float coordX;
    private float coordY;

    private final String title;
    private final Map<Entity, ValueCoordY> valueCoords = new HashMap<>();

    public SelectablePoint(String title) {
        this.title = title;
    }

    public void addEntity(Entity entity, int value) {
        valueCoords.put(entity, new ValueCoordY(value));
    }

    @SuppressWarnings("all")
    public void updatePoint(float coordX, float coordY, Entity entity) {
        ValueCoordY valueCoordY = valueCoords.get(entity);

        this.coordX = coordX;
        valueCoordY.coordY = coordY;
    }

    public String getTitle() {
        return title;
    }

    public float getCoordX() {
        return coordX;
    }

    public ValueCoordY getValue(Entity entity) {
        return valueCoords.get(entity);
    }

    public Map<Entity, ValueCoordY> getValues() {
        return valueCoords;
    }

    public static class ValueCoordY {

        public float coordY;
        public final int value;

        public ValueCoordY(int value) {
            this.value = value;
        }
    }
}
