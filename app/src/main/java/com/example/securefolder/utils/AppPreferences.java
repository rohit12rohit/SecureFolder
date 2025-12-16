package com.example.securefolder.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREF_NAME = "SecureVaultPrefs";
    private static final String KEY_SETUP_DONE = "is_setup_done";
    private static final String KEY_PASSWORD_HASH = "password_hash"; // We store hash, not plain text
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isSetupDone() {
        return prefs.getBoolean(KEY_SETUP_DONE, false);
    }

    public void setSetupDone(boolean done) {
        prefs.edit().putBoolean(KEY_SETUP_DONE, done).apply();
    }

    public void savePasswordHash(String hash) {
        prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply();
    }

    public String getPasswordHash() {
        return prefs.getString(KEY_PASSWORD_HASH, null);
    }

    public void incrementFailedAttempts() {
        int current = getFailedAttempts();
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current + 1).apply();
    }

    public void resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
    }

    public int getFailedAttempts() {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0);
    }
}