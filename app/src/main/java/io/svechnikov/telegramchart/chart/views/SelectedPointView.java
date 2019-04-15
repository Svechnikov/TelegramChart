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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

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
    private TextSwitcher title;
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
    private final int rowTopMargin;
    private final int textColor;
    private final ValueAnimator visibilityAnimator;
    private final ValueAnimator translationAnimator;
    private float pendingTranslation;
    private boolean wasShown;
    private Map<Entity, TextSwitcher> viewValues = new HashMap<>();
    private Map<Entity, TextView> viewValuesPercent = new HashMap<>();
    private final int textSize;
    private float prevX;
    private boolean hasPercentValues;

    private final Animation animInTop;
    private final Animation animOutTop;
    private final Animation animInBottom;
    private final Animation animOutBottom;

    private final Runnable showRunnable = new Runnable() {
        @Override
        public void run() {
            showAfterMeasure = false;
            int width = Math.max(getMeasuredWidth(), getMinimumWidth());
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

    public SelectedPointView(final Context context,
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
        textColor = ta.getColor(1, Color.BLACK);
        backgroundStrokeColor = ta.getColor(2, Color.BLACK);
        ta.recycle();

        setOrientation(VERTICAL);

        final Resources r = getResources();

        rowTopMargin = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_row_margin_top);
        horizontalOffset = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_hor_offset);
        topMargin = r.getDimensionPixelSize(R.dimen.chart_main_chart_plot_top_padding);
        textSize = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_text_size);
        final int titleTextSize = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_title_text_size);

        final int padding = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_padding);

        setPadding(padding, 0, padding, padding);

        animInTop = AnimationUtils.loadAnimation(context, R.anim.slide_in_top);
        animInBottom = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        animOutBottom = AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom);
        animOutTop = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        title = new TextSwitcher(context);
        title.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView title = new TextView(context);
                title.setTypeface(null, Typeface.BOLD);
                title.setPadding(0, padding, 0, padding);
                title.setTextColor(textColor);
                title.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        titleTextSize);
                return title;
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(title, lp);

        cornersRadius = r.getDimension(
                R.dimen.chart_selected_point_corner_radius);
        backgroundStrokeWidth = r.getDimensionPixelSize(
                R.dimen.chart_selected_point_stroke_width);

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

    public void setHasPercentValues(boolean hasPercentValues) {
        this.hasPercentValues = hasPercentValues;
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
        int width = getMeasuredWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (width == 0) {
            setMinimumWidth((int)(getMeasuredWidth()* 1.3f));
        }

        if (getMeasuredWidth() > 0 && showAfterMeasure) {
            doShow(true);
        }
    }

    public void setPoint(SelectedPoint point) {
        selectedPoint = point;
    }

    public void show(boolean animate) {
        boolean wasShowing = showing;
        if (!showing) {
            showing = true;
            setVisibility(VISIBLE);
            if (animate) {
                setAlpha(0);
                animateShow();
            }
            title.setCurrentText(selectedPoint.title);
        }
        else {
            Animation animIn;
            Animation animOut;
            if (selectedPoint.x > prevX) {
                animIn = animInTop;
                animOut = animOutBottom;
            }
            else {
                animIn = animInBottom;
                animOut = animOutTop;
            }
            title.setInAnimation(animIn);
            title.setOutAnimation(animOut);
            title.setText(selectedPoint.title);
        }

        prevX = selectedPoint.x;
        if (currentEntitiesCount == -1) {
            createEntitiesContainer();
        }
        fillEntitiesContainer(wasShowing);

        if (getMeasuredWidth() == 0) {
            removeCallbacks(hideRunnable);
            showAfterMeasure = true;
        }
        else {
            doShow(false);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void fillEntitiesContainer(boolean animate) {
        List<Entity> entities = new ArrayList<>(selectedPoint.values.keySet());

        for (Entity entity: entities) {
            String value = String.valueOf(selectedPoint.values.get(entity));
            TextSwitcher switcher = viewValues.get(entity);
            if (animate) {
                switcher.setText(value);
            }
            else {
                switcher.setCurrentText(value);
            }

            if (hasPercentValues) {
                String percentValue = selectedPoint.percentValues.get(entity) + "%";
                viewValuesPercent.get(entity).setText(percentValue);
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

        LinearLayout.LayoutParams lineLayoutParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lineLayoutParams.bottomMargin = rowTopMargin;

        int percentPadding = 0;
        if (hasPercentValues) {
            percentPadding = getResources()
                    .getDimensionPixelSize(
                            R.dimen.chart_selected_point_percent_padding_right);
        }
        for (final Entity entity: selectedPoint.values.keySet()) {
            TextView percentValueView = null;

            if (hasPercentValues) {
                percentValueView = new TextView(getContext());
                percentValueView.setTextColor(textColor);
                percentValueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                percentValueView.setTypeface(null, Typeface.BOLD);
                percentValueView.setId(R.id.selectedPointPercentValue);
                percentValueView.setPadding(0, 0, percentPadding, 0);
                percentValueView.setText("100%");
                percentValueView.measure(0, 0);
                percentValueView.setLayoutParams(
                        new RelativeLayout.LayoutParams(percentValueView.getMeasuredWidth(),
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                percentValueView.setGravity(Gravity.RIGHT);
                percentValueView.setSingleLine(true);
                viewValuesPercent.put(entity, percentValueView);
                percentValueView.setText(null);
            }

            TextView labelView = new TextView(getContext());
            labelView.setText(entity.title);
            labelView.setTextColor(textColor);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            labelView.setId(R.id.selectedPointLabel);

            if (hasPercentValues) {
                RelativeLayout.LayoutParams lp =
                        new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.addRule(
                        RelativeLayout.RIGHT_OF, R.id.selectedPointPercentValue);
                labelView.setLayoutParams(lp);
            }

            TextSwitcher valueView = new TextSwitcher(getContext());
            valueView.setFactory(new ViewSwitcher.ViewFactory() {
                @Override
                public View makeView() {
                    TextView textView = new TextView(getContext());
                    textView.setTypeface(null, Typeface.BOLD);
                    textView.setTextColor(entity.color);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    textView.setGravity(Gravity.RIGHT);
                    textView.setLayoutParams(lp);
                    return textView;
                }
            });

            Animation inAnimation = new AlphaAnimation(0, 1);
            inAnimation.setDuration(250);
            Animation outAnimation = new AlphaAnimation(1, 0);
            outAnimation.setDuration(250);
            valueView.setInAnimation(inAnimation);
            valueView.setOutAnimation(outAnimation);

            viewValues.put(entity, valueView);

            RelativeLayout container = new RelativeLayout(getContext());
            if (percentValueView != null) {
                container.addView(percentValueView);
            }
            container.addView(labelView);

            RelativeLayout.LayoutParams alignRightLayoutParams =
                    new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            alignRightLayoutParams.addRule(
                    RelativeLayout.RIGHT_OF, R.id.selectedPointLabel);

            container.addView(valueView, alignRightLayoutParams);

            addView(container, lineLayoutParams);
        }
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
