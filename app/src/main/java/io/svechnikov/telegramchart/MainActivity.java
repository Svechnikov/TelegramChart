package io.svechnikov.telegramchart;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import io.svechnikov.telegramchart.chart.ChartsRepo;
import io.svechnikov.telegramchart.chart.ServiceLocator;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.ChartViewState;
import io.svechnikov.telegramchart.chart.data.ChartsViewState;
import io.svechnikov.telegramchart.chart.views.ChartView;
import timber.log.Timber;

public class MainActivity extends FragmentActivity {

    private ViewGroup container;
    private ViewGroup scrollView;

    private final List<ChartView> chartViews = new ArrayList<>();

    private ChartsViewState savedChartsViewState;

    private static final String EXTRA_CHARTS_STATE = "io.svechnikov.telegramchart.charts_state";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.plant(new Timber.DebugTree());

        int mode = AppCompatDelegate.getDefaultNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.AppThemeDark);
        }

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_CHARTS_STATE)) {
            savedChartsViewState = intent.getParcelableExtra(EXTRA_CHARTS_STATE);
        }

        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);

        // todo use RecyclerView
        scrollView = findViewById(R.id.scrollView);

        View themeSwitch = findViewById(R.id.themeSwitch);
        themeSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int oldMode = AppCompatDelegate.getDefaultNightMode();
                int newMode;

                if (oldMode == AppCompatDelegate.MODE_NIGHT_YES) {
                    newMode = AppCompatDelegate.MODE_NIGHT_NO;
                }
                else {
                    newMode = AppCompatDelegate.MODE_NIGHT_YES;
                }
                AppCompatDelegate.setDefaultNightMode(newMode);

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

            return chartsRepo.getCharts();
        }

        @Override
        protected void onPostExecute(List<ChartData> chartsData) {
            super.onPostExecute(chartsData);

            final MainActivity activity = weakActivity.get();
            if (activity == null) {
                return;
            }

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
                chartView.setHorizontalItemsCount(6);
                chartView.setVerticalItemsCount(6);
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
