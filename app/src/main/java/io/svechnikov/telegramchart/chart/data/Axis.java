package io.svechnikov.telegramchart.chart.data;

import java.util.Date;

public class Axis {

    public final Point[] values;

    public Axis(Point[] values) {
        this.values = values;
    }

    public int size() {
        return values.length;
    }

    public static class Point {

        public final int index;
        public final String selectedName;
        public final String boundName;
        public final String shortName;
        public final Date date;

        public Point(int index,
                     String selectedName,
                     String boundName,
                     String shortName,
                     Date date) {
            this.index = index;
            this.selectedName = selectedName;
            this.boundName = boundName;
            this.shortName = shortName;
            this.date = date;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Point && ((Point) obj).index == index;
        }

        @Override
        public String toString() {
            return "Point{" +
                    "index=" + index +
                    ", selectedName='" + selectedName + '\'' +
                    ", shortName='" + shortName + '\'' +
                    '}';
        }
    }
}
