package io.svechnikov.telegramchart.chart.data;

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
        public final String fullName;
        public final String shortName;

        public Point(int index, String fullName, String shortName) {
            this.index = index;
            this.fullName = fullName;
            this.shortName = shortName;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Point && ((Point) obj).index == index;
        }

        @Override
        public String toString() {
            return "Point{" +
                    "index=" + index +
                    ", fullName='" + fullName + '\'' +
                    ", shortName='" + shortName + '\'' +
                    '}';
        }
    }
}
