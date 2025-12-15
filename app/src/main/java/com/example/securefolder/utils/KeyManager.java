package com.example.securefolder.utils;

import android.content.Context;
import android.util.Base64;
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

    // --- PRIMARY SETUP (PASSWORD) ---
    public static boolean setupVault(Context context, String password) {
        try {
            // 1. Generate Salt
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            // 2. Generate Random Master Key (DEK)
            byte[] rawMasterKey = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(rawMasterKey);
            SecretKey masterKey = new SecretKeySpec(rawMasterKey, "AES");

            // 3. Encrypt DEK with Password
            SecretKey wrapperKey = deriveWrapperKey(password, salt);
            Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey);
            byte[] iv = cipher.getIV();
            byte[] encryptedMasterKey = cipher.doFinal(rawMasterKey);

            // 4. Save
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

    // --- RECOVERY SETUP (CODE) ---
    public static boolean setupRecovery(Context context, String recoveryCode) {
        try {
            if (cachedMasterKey == null) return false;

            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            SecretKey wrapperKey = deriveWrapperKey(recoveryCode, salt);

            Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey);
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

    // --- UNLOCK (PASSWORD) ---
    public static boolean unlockVault(Context context, String password) {
        try {
            AppPreferences prefs = new AppPreferences(context);
            String saltStr = prefs.getSalt();
            String blobStr = prefs.getMasterKeyBlob();
            String ivStr = prefs.getMasterKeyIV();

            if (saltStr == null || blobStr == null || ivStr == null) return false;

            byte[] salt = Base64.decode(saltStr, Base64.NO_WRAP);
            byte[] blob = Base64.decode(blobStr, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivStr, Base64.NO_WRAP);

            SecretKey wrapperKey = deriveWrapperKey(password, salt);
            cachedMasterKey = decryptKey(blob, wrapperKey, iv);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- UNLOCK (RECOVERY) ---
    public static boolean unlockWithRecovery(Context context, String recoveryCode) {
        try {
            AppPreferences prefs = new AppPreferences(context);
            String saltStr = prefs.getRecoverySalt();
            String blobStr = prefs.getRecoveryBlob();
            String ivStr = prefs.getRecoveryIV();

            if (saltStr == null || blobStr == null || ivStr == null) return false;

            byte[] salt = Base64.decode(saltStr, Base64.NO_WRAP);
            byte[] blob = Base64.decode(blobStr, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivStr, Base64.NO_WRAP);

            SecretKey wrapperKey = deriveWrapperKey(recoveryCode, salt);
            cachedMasterKey = decryptKey(blob, wrapperKey, iv);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- CHANGE PASSWORD ---
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

    // --- UTILS ---
    public static void clearKey() {
        cachedMasterKey = null;
    }

    public static SecretKey getMasterKey() {
        return cachedMasterKey;
    }

    private static SecretKey decryptKey(byte[] blob, SecretKey wrapperKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, wrapperKey, spec);
        byte[] rawMasterKey = cipher.doFinal(blob);
        return new SecretKeySpec(rawMasterKey, "AES");
    }

    private static SecretKey deriveWrapperKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}