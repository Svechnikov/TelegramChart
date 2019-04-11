package io.svechnikov.telegramchart.chart.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.SelectedPoint;

public class SelectedPointView extends LinearLayout {

    private SelectedPoint selectedPoint;
    private TextView title;
    private boolean animationCanceled;
    private boolean showing;
    private boolean showAfterMeasure;
    private final Paint backgroundPaint = new Paint();
    private final int backgroundColor;
    private final int backgroundStrokeColor;
    private final float cornersRadius;
    private final int backgroundStrokeWidth;
    private final int topMargin;
    private final RectF rect = new RectF();
    private final int horizontalOffset;
    private int currentEntitiesCount = -1;
    private final int valueTextSize;
    private final int valueMarkTextSize;
    private final int valueMarginRight;
    private final int rowTopMargin;
    private final ValueAnimator visibilityAnimator;
    private final ValueAnimator translationAnimator;
    private float pendingTranslation;
    private boolean wasShown;

    private static final int COLUMNS_COUNT = 2;

    private final Runnable showRunnable = new Runnable() {
        @Override
        public void run() {
            showAfterMeasure = false;
            int width = getMeasuredWidth();
            float x = selectedPoint.x;
            int parentWidth = ((View)getParent()).getMeasuredWidth();
            float translationX;

            if (x >= parentWidth / 2) {
                translationX = x - width - horizontalOffset;
            }
            else {
                translationX = x + horizontalOffset;
            }
            setTranslationY(topMargin / 2f);
            if (translationX < 0) {
                translationX = 0;
            }
            if (translationX + width > parentWidth) {
                translationX = parentWidth - width;
            }
            if (!wasShown) {
                wasShown = true;
                setTranslationX(translationX);
            }
            else {
                animateTranslation(translationX);
            }
        }
    };

    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            if (showing) {
                currentEntitiesCount = -1;
                showing = false;
                removeCallbacks(showRunnable);
                animateHide();
            }
        }
    };

    private void animateTranslation(float translation) {
        if (translationAnimator.isRunning()) {
            pendingTranslation = translation;
        }
        else {
            translationAnimator.setFloatValues(getTranslationX(), translation);
            translationAnimator.start();
        }
    }

    public SelectedPointView(Context context) {
        this(context, null);
    }

    public SelectedPointView(Context context,
                             @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SelectedPointView(Context context,
                             @Nullable AttributeSet attrs,
                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setLayerType(LAYER_TYPE_HARDWARE, null);

        int[] style = {
                R.attr.selectedPointInfoBackground,
                R.attr.selectedPointInfoTextColor,
                R.attr.selectedPointInfoStrokeColor};
        TypedArray ta = context.obtainStyledAttributes(style);
        backgroundColor = ta.getColor(0, Color.WHITE);
        int titleColor = ta.getColor(1, Color.BLACK);
        backgroundStrokeColor = ta.getColor(2, Color.BLACK);
        ta.recycle();

        setOrientation(VERTICAL);

        Resources r = getResources();

        rowTopMargin = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_row_top_margin);
        valueTextSize = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_value_text_size);
        valueMarkTextSize = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_value_mark_text_size);
        horizontalOffset = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_hor_offset);
        topMargin = r.getDimensionPixelSize(R.dimen.chart_main_chart_plot_top_padding);
        valueMarginRight = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_value_margin_right);

        int padding = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_padding);

        setPadding(padding, padding, padding, padding);

        title = new TextView(context);
        title.setTextColor(titleColor);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                r.getDimensionPixelSize(
                        R.dimen.chart_selected_point_info_title_text_size));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(title, lp);

        cornersRadius = r.getDimension(
                R.dimen.chart_selected_point_info_corner_radius);
        backgroundStrokeWidth = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_info_stroke_width);

        setVisibility(GONE);

        visibilityAnimator = ValueAnimator.ofFloat();
        visibilityAnimator
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        setAlpha((float)animation.getAnimatedValue());
                    }
                });
        visibilityAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                animationCanceled = false;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!animationCanceled) {
                    if (getAlpha() == 0) {
                        setVisibility(GONE);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animationCanceled = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        translationAnimator = ValueAnimator.ofFloat();
        translationAnimator.setDuration(100);
        translationAnimator
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        setTranslationX((float)animation.getAnimatedValue());
                    }
                });
        translationAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                pendingTranslation = -1;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (pendingTranslation > -1) {
                    animateTranslation(pendingTranslation);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                pendingTranslation = -1;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    private void animateShow() {
        visibilityAnimator.setFloatValues(0, 1);
        visibilityAnimator.start();
    }

    private void animateHide() {
        setAlpha(0);

        // todo postpone setting TYPE_LAYER_HARDWARE on MainPlotView
        // while this animation is running
        /*visibilityAnimator.setFloatValues(1, 0);
        visibilityAnimator.start();*/
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        backgroundPaint.setColor(backgroundColor);

        rect.left = 0;
        rect.right = getWidth();
        rect.top = 0;
        rect.bottom = getHeight();

        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);
        canvas.drawRoundRect(rect, cornersRadius,
                cornersRadius, backgroundPaint);

        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setColor(backgroundStrokeColor);
        backgroundPaint.setStrokeWidth(backgroundStrokeWidth);

        // todo use 9-path images for shadow.
        // Painting shadows with Paint requires TYPE_LAYER_SOFTWARE
        // which is very slow.
        // For now no shadow at all
        canvas.drawRoundRect(rect, cornersRadius,
                cornersRadius, backgroundPaint);

        super.dispatchDraw(canvas);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (getMeasuredWidth() > 0 && showAfterMeasure) {
            doShow(true);
        }
    }

    public void setPoint(SelectedPoint point) {
        selectedPoint = point;
    }

    public void show(boolean animate) {
        if (!showing) {
            showing = true;
            setVisibility(VISIBLE);
            if (animate) {
                setAlpha(0);
                animateShow();
            }
        }
        title.setText(selectedPoint.title);
        if (currentEntitiesCount == -1) {
            createEntitiesContainer();
        }
        else {
            fillEntitiesContainer();
        }

        if (getMeasuredWidth() == 0) {
            removeCallbacks(hideRunnable);
            showAfterMeasure = true;
        }
        else {
            doShow(false);
        }
    }

    private Map<Entity, TextView[]> viewValues = new HashMap<>();

    private void fillEntitiesContainer() {
        List<Entity> entities = new ArrayList<>(selectedPoint.values.keySet());

        for (Entity entity: entities) {
            TextView[] values = viewValues.get(entity);
            if (values != null && values.length == 2) {
                values[0].setText(String.valueOf(selectedPoint.values.get(entity)));
                values[1].setText(entity.title);
            }
        }
    }

    private void createEntitiesContainer() {
        currentEntitiesCount = selectedPoint.values.size();

        int childCount = getChildCount();
        for (int i = 1; i < childCount; i++) {
            removeViewAt(1);
        }

        viewValues.clear();

        LinearLayout.LayoutParams simpleLayoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        int rowsCount = (int)Math.ceil((float)currentEntitiesCount / COLUMNS_COUNT);
        List<Entity> entities = new ArrayList<>(selectedPoint.values.keySet());


        LinearLayout columns = new LinearLayout(getContext());
        columns.setOrientation(HORIZONTAL);
        for (int i = 0; i < COLUMNS_COUNT; i++) {
            LinearLayout column = new LinearLayout(getContext());
            column.setOrientation(VERTICAL);
            for (int j = 0; j < rowsCount; j++) {
                int index = j * COLUMNS_COUNT + i;
                if (index == entities.size()) {
                    break;
                }
                Entity entity = entities.get(index);

                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = rowTopMargin;

                TextView value = new TextView(getContext());
                value.setTextSize(TypedValue.COMPLEX_UNIT_PX, valueTextSize);
                value.setTextColor(entity.color);
                value.setText(String.valueOf(selectedPoint.values.get(entity)));
                value.setTypeface(null, Typeface.BOLD);
                value.setSingleLine(true);
                column.addView(value, lp);

                TextView valueMark = new TextView(getContext());
                valueMark.setTextSize(TypedValue.COMPLEX_UNIT_PX, valueMarkTextSize);
                valueMark.setTextColor(entity.color);
                valueMark.setText(entity.title);
                valueMark.setSingleLine(true);
                column.addView(valueMark, simpleLayoutParams);

                viewValues.put(entity, new TextView[]{value, valueMark});
            }
            if (column.getChildCount() == 0) {
                break;
            }
            LinearLayout.LayoutParams lp  = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i < COLUMNS_COUNT - 1) {
                lp.rightMargin = valueMarginRight;
            }
            columns.addView(column, lp);
        }
        addView(columns, simpleLayoutParams);
    }

    private void doShow(boolean now) {
        removeCallbacks(showRunnable);
        removeCallbacks(hideRunnable);
        if (now) {
            showRunnable.run();
        }
        else {
            post(showRunnable);
        }
    }

    public void hide() {
        pendingTranslation = -1;
        translationAnimator.cancel();
        removeCallbacks(showRunnable);
        removeCallbacks(hideRunnable);
        post(hideRunnable);
    }
}
