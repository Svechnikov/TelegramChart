package io.svechnikov.telegramchart.chart;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.svechnikov.telegramchart.R;
import io.svechnikov.telegramchart.chart.data.ChartData;
import io.svechnikov.telegramchart.chart.data.Entity;

public class ChartsRepo {

    private Context context;
    private List<ChartData> cache;

    public ChartsRepo(Context context) {
        this.context = context;
    }

    private List<ChartData> prepareColors(List<ChartData> chartDataList) {
        int theme = ServiceLocator.getInstance(context).settingsRepo()
                .getTheme();
        boolean isDarkTheme = theme == SettingsRepo.THEME_NIGHT;

        for (ChartData chartData: chartDataList) {
            for (Entity entity: chartData.entities) {
                if (isDarkTheme) {
                    entity.color = changeBrightness(entity.originalColor, 0.8f);
                }
                else {
                    entity.color = entity.originalColor;
                }
            }
        }

        return chartDataList;
    }

    private static int changeBrightness(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r,255),
                Math.min(g,255),
                Math.min(b,255));
    }

    public List<ChartData> getCharts() {
        if (cache == null) {
            try {
                ChartParser chartParser = new ChartParser(context);
                List<ChartData> chartsData = new ArrayList<>(5);
                AssetManager assetManager = context.getAssets();

                String[] titles = context.getResources()
                        .getStringArray(R.array.chart_titles);

                for (int i = 1; i < 6; i++) {
                    String overviewFilePath = "contest/" + i + "/overview.json";
                    InputStream in_s = assetManager.open(overviewFilePath);
                    byte[] b = new byte[in_s.available()];
                    in_s.read(b);

                    String title = titles[i - 1];
                    String text = new String(b);
                    chartsData.add(chartParser.parse(title,
                            text, "contest/" + i));
                }
                cache = chartsData;
            } catch (Exception e) {
                throw new RuntimeException("Input data corrupted");
            }
        }
        return prepareColors(cache);
    }
}
