package io.svechnikov.telegramchart.chart.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.Entity;

public class NavigationPlotView extends FrameLayout {

    private final Map<Entity, PlotView> plotViews = new HashMap<>();
    private ValueAnimator animator;
    private Entity animatedEntity;
    private boolean hasDrawn;

    public NavigationPlotView(@NonNull Context context) {
        this(context, null);
    }

    public NavigationPlotView(@NonNull Context context,
                              @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public NavigationPlotView(@NonNull Context context,
                              @Nullable AttributeSet attrs,
                              int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        updatePlotViews();
    }

    public void addEntity(Entity entity) {
        PlotView plotView = new PlotView(getContext());
        plotView.setLayerType(LAYER_TYPE_HARDWARE, null);
        addView(plotView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        plotViews.put(entity, plotView);
        plotView.setEntity(entity);
        updatePlotViews();
    }

    public void onEntityChanged(Entity entity) {
        if (!hasDrawn) {
            updatePlotViews();
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
                updatePlotViews();
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

    private void updatePlotViews() {
        int height = getMeasuredHeight();
        if (height == 0) {
            return;
        }

        int maxValue = calculateMaxValue();

        for (Map.Entry<Entity, PlotView> entrySet: plotViews.entrySet()) {
            Entity entity = entrySet.getKey();
            if (!entity.isVisible() && entity != animatedEntity) {
                continue;
            }
            PlotView plotView = entrySet.getValue();
            if (entity == animatedEntity) {
                float alpha;
                if (entity.isVisible()) {
                    alpha = animator.getAnimatedFraction();
                }
                else {
                    alpha = 1 - animator.getAnimatedFraction();
                }
                plotView.setAlpha(alpha);
            }

            float scale = (float)entity.maxValue() / maxValue;
            plotView.setDrawingScale(scale);
        }
    }

    private int calculateMaxValue() {
        int maxValue = 0;
        int animatedMaxValue = 0;

        for (Entity entity: plotViews.keySet()) {
            if (entity == animatedEntity) {
                animatedMaxValue = entity.maxValue();
                continue;
            }
            if (!entity.isVisible()) {
                continue;
            }
            maxValue = Math.max(maxValue, entity.maxValue());
        }

        if (animatedEntity != null) {
            if (animatedMaxValue > maxValue) {
                int delta = animatedMaxValue - maxValue;
                float fraction = animator.getAnimatedFraction();
                if (!animatedEntity.isVisible()) {
                    // Animating scale up
                    maxValue += delta * (1 - fraction);
                }
                else {
                    // Animating scale down
                    maxValue += delta * fraction;
                }
            }
        }
        return maxValue;
    }

    private class PlotView extends View {

        private Entity entity;
        private final Paint paint = new Paint();
        private float[] lines;
        private float scale = -1;
        private int maxValue;

        public PlotView(Context context) {
            this(context, null);
        }

        public PlotView(Context context,
                        @Nullable AttributeSet attrs) {
            this(context, attrs, -1);
        }

        public PlotView(Context context,
                        @Nullable AttributeSet attrs,
                        int defStyleAttr) {
            super(context, attrs, defStyleAttr);

            setLayerType(LAYER_TYPE_HARDWARE, null);
        }

        public void setDrawingScale(float scale) {
            if (this.scale != scale) {
                this.scale = scale;
                fillLines();
                invalidate();
            }
        }

        public void setEntity(Entity entity) {
            this.entity = entity;
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(entity.color);
            paint.setStrokeWidth(getResources().getDimensionPixelSize(
                    R.dimen.chart_navigation_stroke_width));
            maxValue = entity.maxValue();
        }

        private void fillLines() {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            if (width == 0 || height == 0) {
                return;
            }
            if (lines == null) {
                lines = new float[entity.values.length * 4];
            }
            int[] values = entity.values;
            float scaleX = (float)width / values.length;
            float scaleY = (float)height / maxValue * scale;
            float top = height - scale * height;
            float deltaY1 = top + scaleY * maxValue;
            float deltaY2 = top + scaleY * maxValue;
            for (int i = 0; i < values.length - 1; i++) {
                lines[i * 4] = i * scaleX;
                lines[i * 4 + 1] = deltaY1 - scaleY * values[i];

                lines[i * 4 + 2] = scaleX * i + scaleX;
                lines[i * 4 + 3] = deltaY2 - scaleY * values[i + 1];
            }
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (scale == -1) {
                return;
            }

            hasDrawn = true;
            if (lines == null) {
                fillLines();
            }

            canvas.drawLines(lines, paint);
        }
    }
}
