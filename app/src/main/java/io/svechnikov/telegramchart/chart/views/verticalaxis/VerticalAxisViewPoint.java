package io.svechnikov.telegramchart.chart.views.verticalaxis;

public class VerticalAxisViewPoint {

    private float y;
    private int alpha;
    private String text;
    private int labelBackgroundAlpha;

    public void update(float y,
                       float alpha,
                       String text,
                       float labelBackgroundAlpha) {
        this.y = y;
        this.alpha = (int)(255 * alpha);
        this.labelBackgroundAlpha = (int)(255 * labelBackgroundAlpha);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public float getY() {
        return y;
    }

    public int getAlpha() {
        return alpha;
    }

    public int getLabelBackgroundAlpha() {
        return labelBackgroundAlpha;
    }

    @Override
    public String toString() {
        return "VerticalAxisViewPoint{" +
                "y=" + y +
                ", alpha=" + alpha +
                ", text='" + text + '\'' +
                '}';
    }
}
