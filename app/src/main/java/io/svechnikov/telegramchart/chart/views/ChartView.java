package io.svechnikov.telegramchart.chart.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.NavigationBoundsListener;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.ChartViewState;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.data.NavigationBounds;
import io.svechnikov.telegramchart.chart.views.chart.MainChartView;
import io.svechnikov.telegramchart.chart.views.navigation.NavigationChartView;

public class ChartView extends LinearLayout {

    private ChartData chartData;
    private TextView titleView;
    private TextView boundsRangeTextView;
    private MainChartView mainChartView;
    private NavigationChartView navigationChartView;
    private final int chartTitleColor;
    private boolean noDataMessageShowing;
    private LinearLayout noDataMessageView;
    private ViewGroup checkboxesContainer;
    private final Map<Entity, CoolCheckbox> checkboxes = new HashMap<>();

    public ChartView(Context context) {
        this(context, null);
    }

    public ChartView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int[] style = {
                R.attr.chartBackground,
                R.attr.chartTitleColor};

        TypedArray ta = context.obtainStyledAttributes(style);
        setBackgroundColor(ta.getColor(0, Color.WHITE));
        chartTitleColor = ta.getColor(1, Color.BLACK);
        ta.recycle();

        setOrientation(VERTICAL);

        createTitleContainer();
        createMainChartView();
        createNavigationChartView();

        int paddingTop = context.getResources()
                .getDimensionPixelSize(R.dimen.chart_padding_top);
        setPadding(0, paddingTop, 0, 0);
    }

    private void createCheckboxesContainer() {
        checkboxesContainer = new FlowLayout(getContext());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        Resources r = getResources();
        int horizontalMargin = r.getDimensionPixelSize(
                R.dimen.chart_padding_horizontal);
        int topMargin = r.getDimensionPixelSize(
                R.dimen.chart_checkbox_margin_vertical);

        checkboxesContainer.setPadding(horizontalMargin, topMargin, horizontalMargin, 0);
        addView(checkboxesContainer, lp);
    }

    private void createTitleContainer() {
        RelativeLayout container = new RelativeLayout(getContext());

        Resources r = getResources();
        int paddingHorizontal = r.getDimensionPixelSize(
                R.dimen.chart_padding_horizontal);
        int paddingVertical = r.getDimensionPixelSize(
                R.dimen.chart_padding_horizontal);

        titleView = new TextView(getContext());
        titleView.setPadding(
                paddingHorizontal, paddingVertical,
                paddingHorizontal, paddingVertical);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                r.getDimensionPixelSize(R.dimen.chart_title_size));
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(chartTitleColor);
        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        container.addView(titleView, titleLp);

        boundsRangeTextView = new TextView(getContext());
        boundsRangeTextView.setPadding(
                paddingHorizontal, paddingVertical,
                paddingHorizontal, paddingVertical);
        boundsRangeTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                r.getDimensionPixelSize(R.dimen.chart_bound_range_text_size));
        boundsRangeTextView.setTypeface(null, Typeface.BOLD);
        boundsRangeTextView.setTextColor(chartTitleColor);
        RelativeLayout.LayoutParams boundsRangeLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        boundsRangeLp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        boundsRangeLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        container.addView(boundsRangeTextView, boundsRangeLp);

        addView(container, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
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

        if (checkboxesContainer != null) {
            for (int i = 0; i < checkboxesContainer.getChildCount(); i++) {
                CoolCheckbox checkbox = (CoolCheckbox) checkboxesContainer.getChildAt(i);
                checkbox.setChecked(state.entityVisibility[i]);
            }
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
                List<Entity> entities = chartData.entities;
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

    private boolean hasVisibleEntity() {
        for (Entity item: chartData.entities) {
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
        navigationChartView.addBoundsListener(new NavigationBoundsListener() {
            @Override
            public void onNavigationBoundsChanged(NavigationBounds bounds) {
                int leftIndex = Math.round(bounds.left);
                int rightIndex = Math.round(bounds.right);

                String left = chartData.axis.values[leftIndex].boundName;
                String right = chartData.axis.values[rightIndex].boundName;

                boundsRangeTextView.setText(left + " - " + right);
            }
        });
        Resources r = getResources();
        int height = r.getDimensionPixelSize(
                R.dimen.chart_navigation_chart_height);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, height);
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

    public void setChartData(final ChartData chartData) {
        this.chartData = chartData;

        setTitle(chartData.title);

        mainChartView.setChartData(chartData);
        navigationChartView.setChartData(chartData);

        if (chartData.entities.size() > 1) {
            addCheckboxes();
        }
        else {
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams)navigationChartView.getLayoutParams();
            lp.bottomMargin = getResources().getDimensionPixelSize(
                    R.dimen.chart_navigation_margin_bottom);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void addCheckboxes() {
        createCheckboxesContainer();
        Resources r = getContext().getResources();
        for (final Entity entity: chartData.entities) {
            CoolCheckbox coolCheckbox = new CoolCheckbox(getContext());
            coolCheckbox.setText(entity.title);
            coolCheckbox.setColor(entity.color);
            coolCheckbox.setChecked(true);
            coolCheckbox.setListener(new CoolCheckbox.Listener() {
                @Override
                public void onCheckedChanged(boolean isChecked) {
                    entity.setVisible(isChecked);
                    onEntityChanged(entity);

                    List<Entity> visibleEntities = new ArrayList<>();
                    for (Entity entity: chartData.entities) {
                        if (entity.isVisible()) {
                            visibleEntities.add(entity);
                        }
                    }

                    if (visibleEntities.size() == 1) {
                        checkboxes.get(visibleEntities.get(0)).setUncheckable(false);
                    }
                    else if (visibleEntities.size() == 2 &&
                            chartData.type == ChartData.TYPE_PERCENTAGE) {
                        for (Entity entity: visibleEntities) {
                            checkboxes.get(entity).setUncheckable(false);
                        }
                    }
                    else {
                        for (Entity entity: visibleEntities) {
                            checkboxes.get(entity).setUncheckable(true);
                        }
                    }
                }
            });
            coolCheckbox.setTextSize(r.getDimension(R.dimen.chart_text_size));

            int marginVertical = r.getDimensionPixelSize(
                    R.dimen.chart_checkbox_margin_vertical);
            int marginHorizontal = r.getDimensionPixelSize(
                    R.dimen.chart_checkbox_margin_right);

            FlowLayout.LayoutParams lp =
                    new FlowLayout.LayoutParams(marginHorizontal, marginVertical);
            checkboxesContainer.addView(coolCheckbox, lp);
            checkboxes.put(entity, coolCheckbox);
        }
    }

    public void setTitle(String title) {
        titleView.setText(title);
    }

}
