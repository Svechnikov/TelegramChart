package io.svechnikov.telegramchart.chart.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import io.svechnikov.telegramchart.R;

public class CoolCheckbox extends View {

    private Listener listener;
    private String text;
    private boolean checked;
    private int color;
    private GradientDrawable backgroundShape;
    private float tickWidth;
    private float tickHeight;
    private boolean uncheckable = true;

    private final int paddingVertical;
    private final int paddingHorizontal;

    private final Paint backgroundPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint tickPaint = new Paint();
    private final Rect textBounds = new Rect();
    private Path tickPath;
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private final ValueAnimator animator = ValueAnimator.ofInt(0, 1);
    
    private static final int CHECKED_TEXT_COLOR = Color.WHITE;

    public CoolCheckbox(Context context) {
        this(context, null);
    }

    public CoolCheckbox(Context context,
                        AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public CoolCheckbox(Context context,
                        AttributeSet attrs,
                        int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources r = getResources();
        paddingHorizontal = r.getDimensionPixelSize(
                R.dimen.chart_checkbox_padding_horizontal);
        paddingVertical = r.getDimensionPixelSize(
                R.dimen.chart_checkbox_padding_vertical);

        textPaint.setColor(CHECKED_TEXT_COLOR);
        textPaint.setAntiAlias(true);

        tickPaint.setColor(CHECKED_TEXT_COLOR);
        tickPaint.setAntiAlias(true);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(r.getDimensionPixelSize(
                        R.dimen.chart_checkbox_tick_width));
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!uncheckable) {
                    animateOnUncheckAttempt();
                    return;
                }
                checked = !checked;
                animator.cancel();
                animator.start();
                triggerListener();
            }
        });

        backgroundPaint.setStrokeWidth(
                r.getDimensionPixelSize(
                        R.dimen.chart_checkbox_unchecked_border_size));
        backgroundPaint.setAntiAlias(true);

        animator.setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateBackground();
                invalidate();
            }
        });
    }

    private void animateOnUncheckAttempt() {
        float offset = getWidth() * 0.07f;
        Animation anim = new TranslateAnimation(-offset,offset,0,0);
        anim.setDuration(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(4);
        startAnimation(anim);
    }

    public void setUncheckable(boolean uncheckable) {
        this.uncheckable = uncheckable;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec,
                             int heightMeasureSpec) {
        textPaint.getTextBounds(text, 0, text.length(), textBounds);

        createTickPath();

        Paint.FontMetrics fm = textPaint.getFontMetrics();

        float textHeight = fm.bottom - fm.top + fm.leading;
        int height = paddingVertical * 2 + (int)textHeight;
        int width = (int)(paddingHorizontal * 2.5f + textBounds.width() + tickWidth);

        setMeasuredDimension(width, height);

        updateBackground();
    }

    private void createTickPath() {
        tickPath = new Path();
        tickHeight = textBounds.height();
        tickWidth = tickHeight * 1.15f;
        tickPath.moveTo(0, tickHeight * 0.55f);
        tickPath.lineTo(tickWidth * 0.3f, tickHeight);
        tickPath.lineTo(tickWidth, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = getHeight();
        int width = getWidth();

        matrix.reset();
        matrix.setTranslate(paddingHorizontal,
                (height - tickHeight) / 2f);
        tickPath.transform(matrix, path);

        if (checked) {
            backgroundPaint.setStyle(Paint.Style.FILL);
        }
        else {
            backgroundPaint.setStyle(Paint.Style.STROKE);
        }

        int textColor;
        float y = height / 2f + textBounds.height() / 2f;
        float x;

        if (animator.isStarted()) {
            float xChecked = paddingHorizontal * 1.5f + tickWidth;
            float xUnchecked = (width - textBounds.width()) / 2f;
            float fraction = animator.getAnimatedFraction();

            if (!checked) {
                x = xChecked - (xChecked - xUnchecked) * fraction;
                tickPaint.setAlpha((int)(255 * (1 - fraction)));
                textColor = blendColors(color, CHECKED_TEXT_COLOR, (1 - fraction));
            }
            else {
                x = xChecked - (xChecked - xUnchecked) * (1 - fraction);
                tickPaint.setAlpha((int)(255 * fraction));
                textColor = blendColors(color, CHECKED_TEXT_COLOR, fraction);
            }
        }
        else {
            if (checked) {
                x = paddingHorizontal * 1.5f + tickWidth;
                tickPaint.setAlpha(255);
                textColor = CHECKED_TEXT_COLOR;
            }
            else {
                tickPaint.setAlpha(0);
                x = (width - textBounds.width()) / 2f;
                textColor = color;
            }
        }

        textPaint.setColor(textColor);
        canvas.drawPath(path, tickPaint);
        canvas.drawText(text, x, y, textPaint);
    }

    public void setColor(int color) {
        this.color = color;
        backgroundPaint.setColor(color);
    }

    public void setTextSize(float size) {
        textPaint.setTextSize(size);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();

        triggerListener();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void triggerListener() {
        if (getMeasuredHeight() > 0) {
            updateBackground();
        }

        if (listener != null) {
            listener.onCheckedChanged(checked);
        }
    }

    private void updateBackground() {
        if (backgroundShape == null) {
            backgroundShape = new GradientDrawable();
            backgroundShape.setShape(GradientDrawable.RECTANGLE);
            backgroundShape.setCornerRadius(getMeasuredHeight() / 2f);
            backgroundShape.setStroke(4, color);
            setBackground(backgroundShape);
        }

        int backgroundColor;
        if (animator.isStarted()) {
            float fraction = animator.getAnimatedFraction();
            if (checked) {
                backgroundColor = getColorWithAlpha(color, fraction);
            }
            else {
                backgroundColor = getColorWithAlpha(color, 1 - fraction);
            }
        }
        else {
            if (checked) {
                backgroundColor = color;
            }
            else {
                backgroundColor = Color.TRANSPARENT;
            }
        }

        backgroundShape.setColor(backgroundColor);
    }

    private int getColorWithAlpha(int color, float alpha) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        return Color.argb((int)(255 * alpha), red, green, blue);
    }

    private int blendColors(int from, int to, float ratio) {
        final float inverseRatio = 1f - ratio;

        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;

        return Color.rgb((int) r, (int) g, (int) b);
    }

    public interface Listener {
        void onCheckedChanged(boolean isChecked);
    }
}
