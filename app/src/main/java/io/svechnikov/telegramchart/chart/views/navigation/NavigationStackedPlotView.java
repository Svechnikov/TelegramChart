package io.svechnikov.telegramchart.chart.views.navigation;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;

public class NavigationStackedPlotView extends View implements NavigationPlotView {

    private ValueAnimator animator;
    private Entity animatedEntity;
    private boolean hasDrawn;
    private ChartData chartData;
    private float[][] lines;
    private final Map<Entity, Paint> paints = new HashMap<>();
    private final int marginHorizontal;

    public NavigationStackedPlotView(@NonNull Context context) {
        this(context, null);
    }

    public NavigationStackedPlotView(@NonNull Context context,
                                     @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NavigationStackedPlotView(@NonNull Context context,
                                     @Nullable AttributeSet attrs,
                                     int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        marginHorizontal = getResources().getDimensionPixelSize(
                R.dimen.chart_padding_horizontal);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!hasDrawn) {
            hasDrawn = true;
            recalculateLines();
        }

        for (int i = 0; i < chartData.entities.size(); i++) {
            Entity entity = chartData.entities.get(i);
            if (entity.isVisible() || entity == animatedEntity) {
                Paint paint = paints.get(entity);
                float[] lines = this.lines[i];

                canvas.drawLines(lines, paint);
            }
        }
    }

    @Override
    public void setChartData(ChartData chartData) {
        this.chartData = chartData;

        int linesCount = 4 * chartData.axis.size();

        lines = new float[chartData.entities.size()][linesCount];

        for (int i = 0; i < chartData.entities.size(); i++) {
            Entity entity = chartData.entities.get(i);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(entity.color);
            paints.put(entity, paint);
        }

        invalidate();
    }

    @SuppressWarnings("ConstantConditions")
    private void recalculateLines() {
        int maxValue = Integer.MIN_VALUE;

        for (int i = 0; i < chartData.axis.size(); i++) {
            int value = 0;

            for (Entity entity: chartData.entities) {
                if (entity == animatedEntity) {
                    if (entity.isVisible()) {
                        value += entity.values[i] * animator.getAnimatedFraction();
                    }
                    else {
                        value += entity.values[i] * (1 - animator.getAnimatedFraction());
                    }
                }
                else if (entity.isVisible()) {
                    value += entity.values[i];
                }
            }

            maxValue = Math.max(maxValue, value);
        }

        float scaleY = (float)getHeight() / maxValue;
        float barWidth = (float)(getWidth() - marginHorizontal * 2) / chartData.axis.size();

        for (Entity entity: chartData.entities) {
            paints.get(entity).setStrokeWidth(barWidth);
        }

        for (int i = 0; i < chartData.axis.size(); i++) {
            float x = i * barWidth;
            float yFrom = getHeight();
            float yTo;

            for (int j = 0; j < chartData.entities.size(); j++) {
                Entity entity = chartData.entities.get(j);
                if (!entity.isVisible() && entity != animatedEntity) {
                    continue;
                }
                float[] lines = this.lines[j];

                if (entity == animatedEntity) {
                    if (entity.isVisible()) {
                        yTo = yFrom - entity.values[i] * scaleY * animator.getAnimatedFraction();
                    }
                    else {
                        yTo = yFrom - entity.values[i] * scaleY * (1 - animator.getAnimatedFraction());
                    }
                }
                else {
                    yTo = yFrom - entity.values[i] * scaleY;
                }

                lines[i * 4] = x + marginHorizontal;
                lines[i * 4 + 1] = yFrom;
                lines[i * 4 + 2] = x + marginHorizontal;
                lines[i * 4 + 3] = yTo;

                yFrom = yTo;
            }
        }
    }

    @Override
    public void onEntityChanged(Entity entity) {
        if (!hasDrawn) {
            invalidate();
            return;
        }
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        animatedEntity = entity;

        animator = ObjectAnimator.ofInt(1, 0)
                .setDuration(200);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                recalculateLines();
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animatedEntity = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }
}
