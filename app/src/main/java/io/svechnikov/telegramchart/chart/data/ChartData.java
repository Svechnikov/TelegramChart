package io.svechnikov.telegramchart.chart.data;

import java.util.List;

public class ChartData {

    public static final int TYPE_Y_SCALED = 0;
    public static final int TYPE_STACKED = 1;
    public static final int TYPE_PERCENTAGE = 2;
    public static final int TYPE_BAR = 3;
    public static final int TYPE_LINE = 4;
    
    public final String title;
    public final List<Entity> entities;
    public final Axis axis;
    public final int type;
    public final String detailsPath;

    public ChartData(String title,
                     List<Entity> entities,
                     Axis axis,
                     int type,
                     String detailsPath) {
        this.title = title;
        this.entities = entities;
        this.axis = axis;
        this.type = type;
        this.detailsPath = detailsPath;
    }
}
