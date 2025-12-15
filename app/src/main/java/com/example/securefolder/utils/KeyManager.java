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
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    // The Active Master Key (Held in RAM only)
    private static SecretKey cachedMasterKey;

    /**
     * INITIAL SETUP:
     * Generates a random Master Key (DEK) and encrypts it with the User's Password.
     */
    public static boolean setupVault(Context context, String password) {
        try {
            // 1. Generate Random Salt
            byte[] salt = generateSalt();
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            // 2. Generate Random Master Key (The real key that encrypts files)
            byte[] rawMasterKey = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(rawMasterKey);
            SecretKey masterKey = new SecretKeySpec(rawMasterKey, "AES");

            // 3. Encrypt the Master Key with Password
            EncryptedBlob blob = encryptKeyWithPassword(masterKey, password, salt);

            // 4. Save to Prefs
            AppPreferences prefs = new AppPreferences(context);
            prefs.saveVaultData(saltStr, blob.ciphertext, blob.iv);

            // Cache it for immediate use
            cachedMasterKey = masterKey;
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ENABLE RECOVERY:
     * Encrypts the currently cached Master Key with the Recovery Code
     * and saves it as a secondary blob.
     */
    public static boolean enableRecovery(Context context, String recoveryCode) {
        if (cachedMasterKey == null) return false;
        try {
            // Generate a fresh salt for recovery
            byte[] salt = generateSalt();
            String saltStr = Base64.encodeToString(salt, Base64.NO_WRAP);

            // Encrypt the Cached Master Key with the Recovery Code
            EncryptedBlob blob = encryptKeyWithPassword(cachedMasterKey, recoveryCode, salt);

            // Save as Recovery Data
            AppPreferences prefs = new AppPreferences(context);
            prefs.saveRecoveryData(saltStr, blob.ciphertext, blob.iv);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * UNLOCK VAULT:
     * Decrypts the Primary Blob using the Password.
     */
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

            SecretKey masterKey = decryptKeyWithPassword(blob, iv, password, salt);
            if (masterKey != null) {
                cachedMasterKey = masterKey;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * RESET PASSWORD WITH RECOVERY CODE:
     * 1. Decrypts the Recovery Blob using the Code.
     * 2. Re-encrypts the Master Key with the NEW Password.
     * 3. Updates the Primary Blob.
     */
    public static boolean resetPasswordWithRecovery(Context context, String recoveryCode, String newPassword) {
        try {
            AppPreferences prefs = new AppPreferences(context);
            String recSalt = prefs.getRecoverySalt();
            String recBlob = prefs.getRecoveryBlob();
            String recIv = prefs.getRecoveryIV();

            if (recSalt == null) return false;

            // 1. Recover Master Key
            byte[] salt = Base64.decode(recSalt, Base64.NO_WRAP);
            byte[] blob = Base64.decode(recBlob, Base64.NO_WRAP);
            byte[] iv = Base64.decode(recIv, Base64.NO_WRAP);

            SecretKey recoveredKey = decryptKeyWithPassword(blob, iv, recoveryCode, salt);
            if (recoveredKey == null) return false; // Wrong code

            // 2. Encrypt with New Password
            byte[] newSalt = generateSalt();
            EncryptedBlob newPrimaryBlob = encryptKeyWithPassword(recoveredKey, newPassword, newSalt);

            // 3. Save New Primary
            prefs.saveVaultData(
                    Base64.encodeToString(newSalt, Base64.NO_WRAP),
                    newPrimaryBlob.ciphertext,
                    newPrimaryBlob.iv
            );

            cachedMasterKey = recoveredKey;
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * CHANGE PASSWORD:
     * Re-encrypts the Master Key with a new password.
     */
    public static boolean changePassword(Context context, String newPassword) {
        if (cachedMasterKey == null) return false;
        try {
            byte[] newSalt = generateSalt();
            EncryptedBlob blob = encryptKeyWithPassword(cachedMasterKey, newPassword, newSalt);

            AppPreferences prefs = new AppPreferences(context);
            prefs.saveVaultData(
                    Base64.encodeToString(newSalt, Base64.NO_WRAP),
                    blob.ciphertext,
                    blob.iv
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- HELPERS ---

    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static EncryptedBlob encryptKeyWithPassword(SecretKey masterKey, String password, byte[] salt) throws Exception {
        // Derive KEK
        SecretKey wrapperKey = deriveWrapperKey(password, salt);

        // Encrypt
        Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, wrapperKey);
        byte[] iv = cipher.getIV();
        byte[] encryptedBytes = cipher.doFinal(masterKey.getEncoded());

        return new EncryptedBlob(
                Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                Base64.encodeToString(iv, Base64.NO_WRAP)
        );
    }

    private static SecretKey decryptKeyWithPassword(byte[] blob, byte[] iv, String password, byte[] salt) throws Exception {
        SecretKey wrapperKey = deriveWrapperKey(password, salt);

        Cipher cipher = Cipher.getInstance(KEK_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
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

    public static SecretKey getMasterKey() {
        return cachedMasterKey;
    }

    public static void clearKey() {
        cachedMasterKey = null;
    }

    private static class EncryptedBlob {
        String ciphertext;
        String iv;
        EncryptedBlob(String c, String i) { this.ciphertext = c; this.iv = i; }
    }
}