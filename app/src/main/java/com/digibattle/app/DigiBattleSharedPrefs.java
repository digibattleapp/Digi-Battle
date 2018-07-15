package com.digibattle.app;

import android.content.Context;
import android.content.SharedPreferences;

public class DigiBattleSharedPrefs {

    private static final String SHARED_PREF_NAME = "digimon_fragment_pref";
    private static final String LAST_VERSION_POSITION = "last_version_position";
    private static final String LAST_ADVANCED_MESSAGES = "last_advanced_messages";

    private static DigiBattleSharedPrefs sInstance;
    private SharedPreferences mSharedPreferences;

    private DigiBattleSharedPrefs(Context context) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public synchronized static DigiBattleSharedPrefs getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DigiBattleSharedPrefs(context);
        }
        return sInstance;
    }

    public void setLastVersionPosition(int pos) {
        mSharedPreferences.edit().putInt(LAST_VERSION_POSITION, pos).apply();
    }

    public int getLastVersionPosition() {
        return mSharedPreferences.getInt(LAST_VERSION_POSITION, 0);
    }

    public void setLastAdvancedMessages(String messages) {
        mSharedPreferences.edit().putString(LAST_ADVANCED_MESSAGES, messages).apply();
    }

    public String getLastAdvancedMessages() {
        return mSharedPreferences.getString(LAST_ADVANCED_MESSAGES,
                "0000000000000000000000000000000000000000");
    }
}
