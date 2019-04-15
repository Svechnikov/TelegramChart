package io.svechnikov.telegramchart.chart;

import android.content.Context;

public class ServiceLocator {

    private static ServiceLocator instance;

    private final ChartsRepo chartsRepo;
    private final SettingsRepo settingsRepo;

    public static ServiceLocator getInstance(Context context) {
        if (instance == null) {
            instance = new ServiceLocator(context);
        }
        return instance;
    }

    public ServiceLocator(Context context) {
        chartsRepo = new ChartsRepo(context);
        settingsRepo = new SettingsRepo(context);
    }

    public ChartsRepo chartsRepo() {
        return chartsRepo;
    }

    public SettingsRepo settingsRepo() {
        return settingsRepo;
    }
}
