package io.svechnikov.telegramchart.chart;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;

public class ChartParser {

    private final SimpleDateFormat selectedDateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH);

    private final SimpleDateFormat boundDateFormat =
            new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);

    private final SimpleDateFormat shortDateFormat =
            new SimpleDateFormat("MMM dd", Locale.ENGLISH);

    public ChartData parse(String title,
                           String chartJson,
                           String detailsPath) throws JSONException {
        JSONObject jsonObject = new JSONObject(chartJson);

        JSONObject types = jsonObject.getJSONObject("types");
        JSONObject colors = jsonObject.getJSONObject("colors");
        JSONObject names = jsonObject.getJSONObject("names");
        JSONArray columns = jsonObject.getJSONArray("columns");

        boolean yScaled = jsonObject.has("y_scaled") &&
                jsonObject.getBoolean("y_scaled");

        boolean stacked = jsonObject.has("stacked") &&
                jsonObject.getBoolean("stacked");

        boolean percentage = jsonObject.has("percentage") &&
                jsonObject.getBoolean("percentage");

        boolean isBar = false;

        Axis.Point[] horizontalAxisValues = null;
        List<Entity> entities = new ArrayList<>();

        for (int i = 0; i < columns.length(); i++) {
            JSONArray column = columns.getJSONArray(i);
            String id = column.getString(0);
            String type = types.getString(id);

            if (type.equals("bar")) {
                isBar = true;
            }

            switch (type) {
                case "x":
                    horizontalAxisValues = new Axis.Point[column.length() - 1];
                    for (int j = 1; j < column.length(); j++) {
                        Date date = new Date(column.getLong(j));

                        String selectedName = selectedDateFormat.format(date);
                        String boundName = boundDateFormat.format(date);

                        String shortName = shortDateFormat.format(date);
                        int pointIndex = j - 1;
                        horizontalAxisValues[pointIndex] =
                                new Axis.Point(pointIndex,
                                        selectedName,
                                        boundName,
                                        shortName,
                                        date);
                    }
                    break;
                case "line":
                case "bar":
                case "area":
                    String entityTitle = names.getString(id);
                    int color = Color.parseColor(colors.getString(id));
                    int values[] = new int[column.length() - 1];
                    for (int j = 1; j < column.length(); j++) {
                        values[j - 1] = column.getInt(j);
                    }
                    entities.add(new Entity(color, entityTitle, values));
                    break;
            }
        }

        int type;

        if (yScaled) {
            type = ChartData.TYPE_Y_SCALED;
        }
        else if (stacked) {
            if (percentage) {
                type = ChartData.TYPE_PERCENTAGE;
            }
            else {
                type = ChartData.TYPE_STACKED;
            }
        }
        else if (isBar) {
            type = ChartData.TYPE_BAR;
        }
        else {
            type = ChartData.TYPE_LINE;
        }

        Axis axisX = new Axis(horizontalAxisValues);
        return new ChartData(title, entities, axisX, type, detailsPath);
    }
}
