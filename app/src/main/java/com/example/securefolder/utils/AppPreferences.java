package com.example.securefolder.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREF_NAME = "SecureFolderPrefs";

    // Auth Flags
    private static final String KEY_IS_SETUP_DONE = "is_setup_done";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCK_TIMEOUT = "lock_timeout_ms";

    // KEK Architecture (Primary Password)
    private static final String KEY_SALT = "auth_salt";
    private static final String KEY_MASTER_BLOB = "master_key_blob";
    private static final String KEY_MASTER_IV = "master_key_iv";

    // KEK Architecture (Recovery Code)
    private static final String KEY_RECOVERY_BLOB = "recovery_key_blob";
    private static final String KEY_RECOVERY_IV = "recovery_key_iv";
    private static final String KEY_RECOVERY_SALT = "recovery_salt";

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

    public void saveRecoveryData(String salt, String encryptedKeyBlob, String iv) {
        sharedPreferences.edit()
                .putString(KEY_RECOVERY_SALT, salt)
                .putString(KEY_RECOVERY_BLOB, encryptedKeyBlob)
                .putString(KEY_RECOVERY_IV, iv)
                .apply();
    }

    // Getters for Primary Auth
    public String getSalt() { return sharedPreferences.getString(KEY_SALT, null); }
    public String getMasterKeyBlob() { return sharedPreferences.getString(KEY_MASTER_BLOB, null); }
    public String getMasterKeyIV() { return sharedPreferences.getString(KEY_MASTER_IV, null); }

    // Getters for Recovery Auth
    public String getRecoverySalt() { return sharedPreferences.getString(KEY_RECOVERY_SALT, null); }
    public String getRecoveryBlob() { return sharedPreferences.getString(KEY_RECOVERY_BLOB, null); }
    public String getRecoveryIV() { return sharedPreferences.getString(KEY_RECOVERY_IV, null); }

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

    public void setLockTimeout(long millis) {
        sharedPreferences.edit().putLong(KEY_LOCK_TIMEOUT, millis).apply();
    }

    public long getLockTimeout() {
        return sharedPreferences.getLong(KEY_LOCK_TIMEOUT, 0);
    }

    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }
}