package io.svechnikov.telegramchart.chart;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsRepo {

    public static final int THEME_DAY = 1;
    public static final int THEME_NIGHT = 2;

    private static final String PREFS_NAME = "io.svechnikov.telegramchart.prefs";
    private static final String THEME_NAME = "io.svechnikov.telegramchart.prefs.theme";

    private final SharedPreferences prefs;

    public SettingsRepo(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getTheme() {
        return prefs.getInt(THEME_NAME, THEME_DAY);
    }

    public void setTheme(int theme) {
        prefs.edit().putInt(THEME_NAME, theme).apply();
    }
}
