package com.example.securefolder.utils;

import android.content.Context;
import android.util.Base64;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * KeyManager
 * Handles deriving the Master Key from the user's password.
 */
public class KeyManager {

    private static final int ITERATION_COUNT = 10000;
    private static final int KEY_LENGTH = 256; // 256-bit AES

    // We store the derived key in memory only (Singleton pattern)
    private static SecretKey cachedMasterKey;

    /**
     * Derives the AES key from the user's password and stored salt.
     * This must be called immediately after Login.
     */
    public static void generateMasterKey(Context context, String password) {
        try {
            AppPreferences prefs = new AppPreferences(context);
            String saltStr = prefs.getPasswordSalt();

            if (saltStr == null) return;

            byte[] salt = Base64.decode(saltStr, Base64.NO_WRAP);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);

            // Save the key in memory to use for encryption/decryption
            cachedMasterKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the key currently in memory.
     */
    public static SecretKey getMasterKey() {
        return cachedMasterKey;
    }

    /**
     * Clears the key from memory (on logout or lock).
     */
    public static void clearKey() {
        cachedMasterKey = null;
    }
}