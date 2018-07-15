package com.digibattle.app;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

class DigiBattleConfig {

    private final static String TAG = "DigiBattleConfig";

    public static int expectedRTT = 30;
    public static int voltageChangeThreshold = 10000;
    public static float analogInitRatio = 0.7f;
    public static int analogDelta = 50;


    public static void update(Context context) {
        expectedRTT = parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString("expected_rtt",
                        "30"), 30);
        voltageChangeThreshold = parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        "voltage_change_threshold", "10000"), 10000);
        analogInitRatio = parseFloat(
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                        "analog_init_ratio", "0.7"), 0.7f);
        analogDelta = parseInt(
                PreferenceManager.getDefaultSharedPreferences(context).getString("analog_delta",
                        "50"), 50);
        Log.i(TAG,
                "expectedRTT:" + expectedRTT + ", voltageChangeThreshold:" + voltageChangeThreshold
                        + ", analogInitRatio:" + analogInitRatio + ", analogDelta:" + analogDelta);
    }

    private static int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    private static float parseFloat(String str, float defaultValue) {
        try {
            return Float.parseFloat(str);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }
}
