package com.example.securefolder.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREF_NAME = "SecureFolderPrefs";

    // Auth Flags
    private static final String KEY_IS_SETUP_DONE = "is_setup_done";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";

    // KEK Architecture (New)
    private static final String KEY_SALT = "auth_salt"; // For PBKDF2
    private static final String KEY_MASTER_BLOB = "master_key_blob"; // Encrypted Master Key
    private static final String KEY_MASTER_IV = "master_key_iv"; // IV for the Blob

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

    public void saveVaultData(String salt, String encryptedKeyBlob, String iv) {
        sharedPreferences.edit()
                .putString(KEY_SALT, salt)
                .putString(KEY_MASTER_BLOB, encryptedKeyBlob)
                .putString(KEY_MASTER_IV, iv)
                .apply();
    }

    public String getSalt() {
        return sharedPreferences.getString(KEY_SALT, null);
    }

    public String getMasterKeyBlob() {
        return sharedPreferences.getString(KEY_MASTER_BLOB, null);
    }

    public String getMasterKeyIV() {
        return sharedPreferences.getString(KEY_MASTER_IV, null);
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