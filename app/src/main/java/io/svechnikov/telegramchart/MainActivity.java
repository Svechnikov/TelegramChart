package io.svechnikov.telegramchart;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentActivity;
import io.svechnikov.telegramchart.chart.data.Axis;
import io.svechnikov.telegramchart.chart.data.ChartViewState;
import io.svechnikov.telegramchart.chart.data.Entity;
import io.svechnikov.telegramchart.chart.views.ChartView;

// We could have thrown away androidx.appcompat
// to get the final apk size smaller,
// but we would limit ourselves to SDK >= 21
// (otherwise no Toolbar and colored Checkboxes will be available).
// There are still devices on Android 4 out there,
// so I decided to keep androidx.appcompat
//
// There are other ways for extreme optimizations, but I don't think
// it's wise to use them.
public class MainActivity extends FragmentActivity {

    private ViewGroup container;
    private ViewGroup scrollView;

    private final List<ChartView> chartViews = new ArrayList<>();

    private int savedScroll;
    private ChartViewState[] savedChartViewStates;

    private static final String EXTRA_SAVED_SCROLL = "extra_scroll";
    private static final String EXTRA_SAVED_CHARTS_STATE = "extra_charts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int mode = AppCompatDelegate.getDefaultNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.AppThemeDark);
        }
        super.onCreate(savedInstanceState);

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

                recreate();
            }
        });

        readChartData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(EXTRA_SAVED_SCROLL, scrollView.getScrollY());

        ChartViewState[] states = new ChartViewState[chartViews.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = chartViews.get(i).getState();
        }
        outState.putParcelableArray(EXTRA_SAVED_CHARTS_STATE, states);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_SAVED_SCROLL)) {
            savedScroll = savedInstanceState.getInt(EXTRA_SAVED_SCROLL);
        }

        if (savedInstanceState.containsKey(EXTRA_SAVED_CHARTS_STATE)) {
            savedChartViewStates = (ChartViewState[])savedInstanceState
                    .getParcelableArray(EXTRA_SAVED_CHARTS_STATE);
        }
    }

    private void readChartData() {
        new ReadChartData(this).execute((Void) null);
    }

    private static class ReadChartData extends AsyncTask<Void, Void, String> {

        private final WeakReference<MainActivity> weakActivity;

        private final SimpleDateFormat fullDateYearFormat =
                new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.ENGLISH);

        private final SimpleDateFormat fullDateFormat =
                new SimpleDateFormat("EEE, MMM dd", Locale.ENGLISH);

        private final SimpleDateFormat shortDateFormat =
                new SimpleDateFormat("MMM dd", Locale.ENGLISH);

        public ReadChartData(MainActivity context) {
            weakActivity = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                Context context = weakActivity.get();
                if (context == null) {
                    return null;
                }
                Resources res = context.getResources();
                InputStream in_s = res.openRawResource(R.raw.chart_data);

                byte[] b = new byte[in_s.available()];
                in_s.read(b);
                return new String(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result == null) {
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(result);
                final MainActivity activity = weakActivity.get();
                if (activity == null) {
                    return;
                }

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject chartData = jsonArray.getJSONObject(i);
                    addChart(activity, chartData, i);
                }

                activity.scrollView.getViewTreeObserver()
                        .addOnGlobalLayoutListener(
                                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        activity.scrollView
                                .getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        activity.scrollView.setScrollY(activity.savedScroll);
                    }
                });
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void addChart(MainActivity activity,
                              JSONObject chartData,
                              int index) throws JSONException {

            JSONObject types = chartData.getJSONObject("types");
            JSONObject colors = chartData.getJSONObject("colors");
            JSONObject names = chartData.getJSONObject("names");
            JSONArray columns = chartData.getJSONArray("columns");

            Axis.Point[] horizontalAxisValues = null;
            List<Entity> entities = new ArrayList<>();

            for (int i = 0; i < columns.length(); i++) {
                JSONArray column = columns.getJSONArray(i);
                String id = column.getString(0);
                String type = types.getString(id);

                switch (type) {
                    case "x":
                        horizontalAxisValues = new Axis.Point[column.length() - 1];
                        boolean moreThanYear = horizontalAxisValues.length > 365;
                        for (int j = 1; j < column.length(); j++) {
                            Date date = new Date(column.getLong(j));
                            String fullName;
                            if (!moreThanYear) {
                                fullName = fullDateFormat.format(date);
                            }
                            else {
                                fullName = fullDateYearFormat.format(date);
                            }
                            String shortName = shortDateFormat.format(date);
                            int pointIndex = j - 1;
                            horizontalAxisValues[pointIndex] =
                                    new Axis.Point(pointIndex, fullName, shortName);
                        }
                        break;
                    case "line":
                        String title = names.getString(id);
                        int color = Color.parseColor(colors.getString(id));
                        int values[] = new int[column.length() - 1];
                        for (int j = 1; j < column.length(); j++) {
                            values[j - 1] = column.getInt(j);
                        }
                        entities.add(new Entity(color, title, values));
                        break;
                }
            }

            ChartView chartView = new ChartView(activity);
            chartView.setHorizontalItemsCount(6);
            chartView.setVerticalItemsCount(6);
            chartView.setTitle(activity.getString(R.string.chart_indexed_title, (index + 1)));
            chartView.setHorizontalAxis(new Axis(horizontalAxisValues));
            chartView.addEntities(entities);
            if (activity.savedChartViewStates != null &&
                    activity.savedChartViewStates.length > index) {
                ChartViewState state = activity.savedChartViewStates[index];
                chartView.setState(state);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = activity.getResources()
                    .getDimensionPixelSize(R.dimen.chart_margin_bottom);
            activity.container.addView(chartView, lp);

            activity.chartViews.add(chartView);
        }
    }
}
