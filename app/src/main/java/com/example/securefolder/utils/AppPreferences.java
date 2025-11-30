package com.example.securefolder.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AppPreferences
 * Handles saving simple app data (flags, hashes) securely.
 */
public class AppPreferences {

    private static final String PREF_NAME = "SecureFolderPrefs";
    private static final String KEY_IS_SETUP_DONE = "is_setup_done";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static final String KEY_PASSWORD_SALT = "password_salt";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";

    private final SharedPreferences sharedPreferences;

    public AppPreferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setSetupDone(boolean isDone) {
        sharedPreferences.edit().putBoolean(KEY_IS_SETUP_DONE, isDone).apply();
    }

    public boolean isSetupDone() {
        return sharedPreferences.getBoolean(KEY_IS_SETUP_DONE, false);
    }

    public void savePasswordData(String hash, String salt) {
        sharedPreferences.edit()
                .putString(KEY_PASSWORD_HASH, hash)
                .putString(KEY_PASSWORD_SALT, salt)
                .apply();
    }

    public String getPasswordHash() {
        return sharedPreferences.getString(KEY_PASSWORD_HASH, null);
    }

    public String getPasswordSalt() {
        return sharedPreferences.getString(KEY_PASSWORD_SALT, null);
    }

    public void incrementFailedAttempts() {
        int current = getFailedAttempts();
        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, current + 1).apply();
    }

    public void resetFailedAttempts() {
        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
    }

    public int getFailedAttempts() {
        return sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0);
    }

    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }
}