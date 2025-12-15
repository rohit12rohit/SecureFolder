package com.example.securefolder.utils;

import android.content.Context;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {

    private static final int PBKDF2_ITERATIONS = 15000;
    private static final int KEY_LENGTH = 256;
    private static final String KEK_ALGORITHM = "AES/GCM/NoPadding";

    // The Active Master Key (Held in RAM only)
    private static SecretKey cachedMasterKey;

    /**
     * INITIAL SETUP:
     */
    public static boolean setupVault(Context context, String password) {
        try {
            // 1. Generate Random Salt
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            // 2. Generate Random Master Key (DEK)
            byte[] rawMasterKey = new byte[32];
            new SecureRandom().nextBytes(rawMasterKey);
            SecretKey masterKey = new SecretKeySpec(rawMasterKey, "AES");

            // 3. Derive Wrapper Key from Password
            SecretKey wrapperKey = deriveWrapperKey(password, salt);

            // 4. Encrypt the Master Key
            Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedMasterKey = cipher.doFinal(rawMasterKey);

            // 5. Save to Prefs
            AppPreferences prefs = new AppPreferences(context);
            prefs.saveVaultData(
                    saltStr,
                    Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP)
            );

            cachedMasterKey = masterKey;
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ENABLE RECOVERY (NEW):
     * Encrypts the *existing* Master Key with the Recovery Code.
     */
    public static boolean enableRecovery(Context context, String recoveryCode) {
        try {
            if (cachedMasterKey == null) return false;

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            SecretKey recoveryWrapper = deriveWrapperKey(recoveryCode, salt);

            Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, recoveryWrapper);
            byte[] iv = cipher.getIV();
            byte[] encryptedMasterKey = cipher.doFinal(cachedMasterKey.getEncoded());

            AppPreferences prefs = new AppPreferences(context);
            prefs.saveRecoveryData(
                    saltStr,
                    Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP)
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * CHANGE PASSWORD (NEW):
     */
    public static boolean changePassword(Context context, String newPassword) {
        try {
            if (cachedMasterKey == null) return false;

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            SecretKey wrapperKey = deriveWrapperKey(newPassword, salt);

            Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedMasterKey = cipher.doFinal(cachedMasterKey.getEncoded());

            AppPreferences prefs = new AppPreferences(context);
            prefs.saveVaultData(
                    saltStr,
                    Base64.encodeToString(encryptedMasterKey, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP)
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean unlockVault(Context context, String password) {
        AppPreferences prefs = new AppPreferences(context);
        return unlockWithParams(prefs.getSalt(), prefs.getMasterKeyBlob(), prefs.getMasterKeyIV(), password);
    }

    public static boolean unlockWithRecovery(Context context, String recoveryCode) {
        AppPreferences prefs = new AppPreferences(context);
        return unlockWithParams(prefs.getRecoverySalt(), prefs.getRecoveryBlob(), prefs.getRecoveryIV(), recoveryCode);
    }

    // NEW: Important for Auto-Lock
    public static void clearKey() {
        cachedMasterKey = null;
    }

    private static boolean unlockWithParams(String saltStr, String blobStr, String ivStr, String secret) {
        try {
            if (saltStr == null || blobStr == null || ivStr == null) return false;

            byte[] salt = Base64.decode(saltStr, Base64.NO_WRAP);
            byte[] blob = Base64.decode(blobStr, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivStr, Base64.NO_WRAP);

            SecretKey wrapperKey = deriveWrapperKey(secret, salt);

            Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, wrapperKey, spec);

            byte[] rawMasterKey = cipher.doFinal(blob);
            cachedMasterKey = new SecretKeySpec(rawMasterKey, "AES");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static SecretKey deriveWrapperKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static SecretKey getMasterKey() {
        return cachedMasterKey;
    }
}