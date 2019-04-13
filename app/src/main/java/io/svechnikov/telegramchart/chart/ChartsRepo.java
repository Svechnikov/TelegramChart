package io.svechnikov.telegramchart.chart;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.ChartData;

public class ChartsRepo {

    private Context context;
    private List<ChartData> cache;

    public ChartsRepo(Context context) {
        this.context = context;
    }

    public List<ChartData> getCharts() {
        if (cache != null) {
            return cache;
        }
        try {
            ChartParser chartParser = new ChartParser();
            List<ChartData> chartsData = new ArrayList<>(5);
            AssetManager assetManager = context.getAssets();

            for (int i = 1; i < 6; i++) {
                String overviewFilePath = "contest/" + i + "/overview.json";
                InputStream in_s = assetManager.open(overviewFilePath);
                byte[] b = new byte[in_s.available()];
                in_s.read(b);
                String title = context.getString(R.string.chart_indexed_title, i);
                String text = new String(b);
                chartsData.add(chartParser.parse(title,
                        text, "contest/" + i));
            }
            cache = chartsData;
            return chartsData;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
