package io.svechnikov.telegramchart;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import io.svechnikov.telegramchart.chart.ChartsRepo;
import io.svechnikov.telegramchart.chart.ServiceLocator;
import io.svechnikov.telegramchart.chart.SettingsRepo;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.ChartViewState;
import io.svechnikov.telegramchart.chart.data.ChartsViewState;
import io.svechnikov.telegramchart.chart.views.ChartView;

public class MainActivity extends FragmentActivity {

    private ViewGroup container;
    private ViewGroup scrollView;

    private final List<ChartView> chartViews = new ArrayList<>();
    private final Handler handler = new Handler();

    private ProgressBar progressBar;
    private boolean chartsLoaded;

    private ChartsViewState savedChartsViewState;

    private static final String EXTRA_CHARTS_STATE = "io.svechnikov.telegramchart.charts_state";

    private final Runnable showLoading = new Runnable() {
        @Override
        public void run() {
            progressBar.setVisibility(View.VISIBLE);
        }
    };

    private void setUpTheme() {
        int theme = ServiceLocator.getInstance(getApplicationContext()).settingsRepo().getTheme();

        boolean isDark = theme == SettingsRepo.THEME_NIGHT;

        if (isDark) {
            setTheme(R.style.AppThemeDark);
        }

        if (Build.VERSION.SDK_INT >= 23) {
            // We can change both status bar background and text colors
            int flags = getWindow().getDecorView().getSystemUiVisibility();

            if (isDark) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
        else if (Build.VERSION.SDK_INT >= 21) {
            // We cannot change status bar text color
            // so when using light theme we apply fallback background color
            // otherwise white text would be invisible on white background
            if (!isDark) {
                int color = ContextCompat.getColor(this,
                        R.color.fallback_status_bar_light_color);
                getWindow().setStatusBarColor(color);
            }
        }
        // On pre 21 devices we cannot alter neither background, nor text colors
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setUpTheme();

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_CHARTS_STATE)) {
            savedChartsViewState = intent.getParcelableExtra(EXTRA_CHARTS_STATE);
        }

        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);

        container = findViewById(R.id.container);

        // todo use RecyclerView
        scrollView = findViewById(R.id.scrollView);

        View themeSwitch = findViewById(R.id.themeSwitch);
        themeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!chartsLoaded) {
                    return;
                }
                SettingsRepo settingsRepo =
                        ServiceLocator.getInstance(getApplicationContext())
                                .settingsRepo();

                int oldTheme = settingsRepo.getTheme();
                int newTheme;

                if (oldTheme == SettingsRepo.THEME_DAY) {
                    newTheme = SettingsRepo.THEME_NIGHT;
                }
                else {
                    newTheme = SettingsRepo.THEME_DAY;
                }

                settingsRepo.setTheme(newTheme);

                Intent intent = new Intent(MainActivity.this,
                        MainActivity.class);

                intent.putExtra(EXTRA_CHARTS_STATE, createChartsViewState());

                startActivity(intent);

                finish();
            }
        });

        readChartData();
    }

    @Override
    protected void onStop() {
        super.onStop();

        handler.removeCallbacks(showLoading);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(EXTRA_CHARTS_STATE, createChartsViewState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_CHARTS_STATE)) {
            savedChartsViewState = savedInstanceState.getParcelable(EXTRA_CHARTS_STATE);
        }
    }

    private ChartsViewState createChartsViewState() {
        int scroll = scrollView.getScrollY();

        ChartViewState[] states = new ChartViewState[chartViews.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = chartViews.get(i).getState();
        }
        return new ChartsViewState(states, scroll);
    }

    private void readChartData() {
        handler.postDelayed(showLoading, 500);
        new ReadChartData(this).execute((Void) null);
    }

    private static class ReadChartData extends AsyncTask<Void, Void, List<ChartData>> {

        private final WeakReference<MainActivity> weakActivity;
        private final ChartsRepo chartsRepo;

        public ReadChartData(MainActivity context) {
            weakActivity = new WeakReference<>(context);
            chartsRepo = ServiceLocator.getInstance(
                    context.getApplicationContext()).chartsRepo();
        }

        @Override
        protected List<ChartData> doInBackground(Void... voids) {
            Context context = weakActivity.get();
            if (context == null) {
                return null;
            }

            try {
                //Thread.sleep(3000);
            }
            catch (Exception e) {}

            return chartsRepo.getCharts();
        }

        @Override
        protected void onPostExecute(List<ChartData> chartsData) {
            super.onPostExecute(chartsData);

            final MainActivity activity = weakActivity.get();
            if (activity == null) {
                return;
            }

            activity.chartsLoaded = true;
            activity.progressBar.setVisibility(View.GONE);
            activity.container.setVisibility(View.VISIBLE);
            activity.handler.removeCallbacks(activity.showLoading);

            final ChartsViewState chartStates = activity.savedChartsViewState;
            final int savedScroll = chartStates != null ? chartStates.scroll : 0;

            activity.scrollView.getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    activity.scrollView
                                            .getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                    activity.scrollView.setScrollY(savedScroll);
                                }
                            });

            for (int i = 0; i < chartsData.size(); i++) {
                ChartData chartData = chartsData.get(i);

                ChartView chartView = new ChartView(activity);
                if (chartData.type == ChartData.TYPE_PERCENTAGE) {
                    chartView.setVerticalItemsCount(5);
                }
                else {
                    chartView.setVerticalItemsCount(6);
                }
                chartView.setHorizontalItemsCount(6);
                chartView.setChartData(chartData);

                if (chartStates != null &&
                        chartStates.states != null &&
                        chartStates.states.length > i) {
                    ChartViewState state = chartStates.states[i];
                    chartView.setState(state);
                }

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = activity.getResources()
                        .getDimensionPixelSize(R.dimen.chart_margin_top);
                lp.bottomMargin = activity.getResources()
                        .getDimensionPixelSize(R.dimen.chart_margin_bottom);
                activity.container.addView(chartView, lp);

                activity.chartViews.add(chartView);
            }
        }
    }
}
