package io.svechnikov.telegramchart.chart.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.core.widget.CompoundButtonCompat;
import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.ChartViewState;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;

public class ChartView extends LinearLayout {

    private Axis horizontalAxis;
    private final List<Entity> entities = new ArrayList<>();
    private final List<CheckBox> checkBoxes = new ArrayList<>();

    private TextView titleView;
    private MainChartView mainChartView;
    private NavigationChartView navigationChartView;
    private final int chartTitleColor;
    private final int checkboxTextColor;
    private final int dividerColor;
    private boolean noDataMessageShowing;
    private LinearLayout noDataMessageView;

    public ChartView(Context context) {
        this(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(VERTICAL);

        createMainChartView();
        createNavigationChartView();

        int[] style = {
                R.attr.chartBackground,
                R.attr.chartTitleColor,
                R.attr.checkboxTextColor,
                R.attr.dividerColor};

        TypedArray ta = context.obtainStyledAttributes(style);
        setBackgroundColor(ta.getColor(0, Color.WHITE));
        chartTitleColor = ta.getColor(1, Color.BLACK);
        checkboxTextColor = ta.getColor(2, Color.BLACK);
        dividerColor = ta.getColor(3, Color.BLACK);
        ta.recycle();

        int paddingTop = context.getResources()
                .getDimensionPixelSize(R.dimen.chart_padding_top);
        setPadding(0, paddingTop, 0, 0);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setState(ChartViewState state) {
        if (state == null) {
            return;
        }
        navigationChartView.setBounds(state.navigationBounds);

        for (int i = 0; i < state.entityVisibility.length && i < checkBoxes.size(); i++) {
            checkBoxes.get(i).setChecked(state.entityVisibility[i]);
        }

        mainChartView.setSelectedPointIndex(state.selectedPointIndex);

        if (!hasVisibleEntity()) {
            showNoDataMessage();
        }
    }

    public ChartViewState getState() {
        if (navigationChartView != null) {
            NavigationBounds bounds = navigationChartView.getBounds();
            if (bounds != null) {
                boolean[] visibilities = new boolean[entities.size()];
                for (int i = 0; i < entities.size(); i++) {
                    visibilities[i] = entities.get(i).isVisible();
                }
                int selectedPointIndex = mainChartView.getSelectedPointIndex();

                return new ChartViewState(bounds, visibilities, selectedPointIndex);
            }
        }
        return null;
    }

    public void setHorizontalItemsCount(int count) {
        mainChartView.setHorizontalItemsCount(count);
        navigationChartView.setHorizontalItemsCount(count);
    }

    public void setVerticalItemsCount(int count) {
        mainChartView.setVerticalItemsCount(count);
    }

    public void setHorizontalAxis(Axis horizontalAxis) {
        this.horizontalAxis = horizontalAxis;

        mainChartView.setHorizontalAxis(horizontalAxis);
    }

    public void addEntities(List<Entity> entities) {
        for (Entity entity: entities) {
            addEntity(entity);
        }
    }

    public void addEntity(final Entity entity) {
        if (horizontalAxis == null) {
            throw new IllegalStateException("You should set horizontalAxis first");
        }
        if (entity.values.length != horizontalAxis.values.length) {
            throw new IllegalArgumentException(
                    "Entity valuesByEntity must correspond to horizontalAxis " +
                            "marks (they must be equal is size)");
        }
        entities.add(entity);
        mainChartView.addEntity(entity);
        navigationChartView.addEntity(entity);

        Resources r = getContext().getResources();

        if (entities.size() > 1) {
            View divider = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    r.getDimensionPixelSize(R.dimen.chart_checkbox_divider_height));
            divider.setBackgroundColor(dividerColor);
            lp.leftMargin = r.getDimensionPixelSize(
                    R.dimen.chart_checkbox_divider_margin_left);
            addView(divider, lp);
        }
        int[][] states = new int[][] {
                new int[] {-android.R.attr.state_checked},
                new int[] { android.R.attr.state_checked}
        };
        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setText(entity.title);
        ColorStateList colorStateList =
                new ColorStateList(states, new int[]{entity.color, entity.color});
        CompoundButtonCompat.setButtonTintList(checkBox, colorStateList);
        checkBox.setChecked(true);
        checkBox.setTextColor(checkboxTextColor);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                entity.setVisible(isChecked);
                onEntityChanged(entity);
            }
        });
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = r.getDimensionPixelSize(R.dimen.chart_checkbox_margin_top);
        lp.bottomMargin = r.getDimensionPixelSize(R.dimen.chart_checkbox_margin_bottom);
        lp.leftMargin = r.getDimensionPixelSize(R.dimen.chart_margin_left);
        checkBox.setPadding(
                r.getDimensionPixelSize(R.dimen.chart_text_padding_left),
                0, 0, 0);
        checkBox.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                r.getDimension(R.dimen.chart_text_size));
        addView(checkBox, lp);
        checkBoxes.add(checkBox);
    }

    private boolean hasVisibleEntity() {
        for (Entity item: entities) {
            if (item.isVisible()) {
                return true;
            }
        }
        return false;
    }

    private void onEntityChanged(Entity entity) {
        navigationChartView.onEntityChanged(entity);
        mainChartView.onEntityChanged(entity);

        boolean hasVisibleEntity = hasVisibleEntity();
        if (hasVisibleEntity && noDataMessageShowing) {
            hideNoDataMessage();
        }
        else if (!hasVisibleEntity && !noDataMessageShowing) {
            showNoDataMessage();
        }
    }

    private void showNoDataMessage() {
        if (noDataMessageView == null) {
            noDataMessageView = new LinearLayout(getContext());
            Resources r = getResources();
            int height = r.getDimensionPixelSize(R.dimen.chart_main_chart_height) +
                    r.getDimensionPixelSize(R.dimen.chart_navigation_chart_height);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            height);
            addView(noDataMessageView, 2, lp);
            noDataMessageView.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams textViewLp =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView textView = new TextView(getContext());
            textView.setText(r.getString(R.string.no_data));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    r.getDimensionPixelSize(R.dimen.no_data_message_text_size));
            noDataMessageView.addView(textView, textViewLp);

            int[] style = {R.attr.noDataMessageTextColor};
            TypedArray ta = getContext().obtainStyledAttributes(style);
            textView.setTextColor(ta.getColor(0, Color.BLACK));
            ta.recycle();
        }
        noDataMessageShowing = true;
        noDataMessageView.setVisibility(VISIBLE);

        mainChartView.setVisibility(GONE);
        navigationChartView.setVisibility(GONE);
    }

    private void hideNoDataMessage() {
        noDataMessageShowing = false;
        noDataMessageView.setVisibility(GONE);

        mainChartView.setVisibility(VISIBLE);
        navigationChartView.setVisibility(VISIBLE);
    }

    private void createNavigationChartView() {
        navigationChartView = new NavigationChartView(getContext());
        navigationChartView.addBoundsListener(mainChartView);
        navigationChartView.addStateListener(mainChartView);
        Resources r = getResources();
        int height = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_height);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, height);
        int marginHorizontal = r.getDimensionPixelSize(
                R.dimen.chart_padding_horizontal);
        lp.leftMargin = marginHorizontal;
        lp.rightMargin = marginHorizontal;
        addView(navigationChartView, lp);
    }

    private void createMainChartView() {
        mainChartView = new MainChartView(getContext());
        int height = getResources()
                .getDimensionPixelSize(R.dimen.chart_main_chart_height);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        height);
        mainChartView.setLayoutParams(lp);
        addView(mainChartView);
    }

    public void setTitle(String title) {
        Context context = getContext();
        if (titleView == null) {
            Resources r = getResources();
            int paddingHorizontal = r.getDimensionPixelSize(
                    R.dimen.chart_padding_horizontal);
            int paddingVertical = r.getDimensionPixelSize(
                    R.dimen.chart_padding_horizontal);

            titleView = new TextView(context);
            titleView.setPadding(
                    paddingHorizontal, paddingVertical,
                    paddingHorizontal, paddingVertical);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    r.getDimensionPixelSize(R.dimen.chart_title_size));
            titleView.setTextColor(chartTitleColor);
            addView(titleView, 0);
        }
        titleView.setText(title);
    }

}
