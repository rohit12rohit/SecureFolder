package com.example.securefolder.utils;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Handles Hardware-Backed Encryption.
 * No hardcoded keys. No SharedPreferences for keys.
 * Uses Android Keystore System.
 */
public class CryptoManager {

    private static final String KEY_ALIAS = "SecureVaultMasterKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String ALGORITHM = KeyProperties.KEY_ALGORITHM_AES;
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM;
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;
    private static final String TRANSFORMATION = ALGORITHM + "/" + BLOCK_MODE + "/" + PADDING;

    private KeyStore keyStore;

    public CryptoManager() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- KEY MANAGEMENT ---

    private SecretKey getSecretKey() {
        try {
            // Check if key exists
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                generateKey();
            }
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEYSTORE);
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(BLOCK_MODE)
                    .setEncryptionPaddings(PADDING)
                    .setRandomizedEncryptionRequired(true) // Requires IV for every encryption
                    .build();
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- ENCRYPTION / DECRYPTION ---

    /**
     * Encrypts stream. Prepend IV to the output stream.
     */
    public boolean encrypt(InputStream inputStream, OutputStream outputStream) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

            byte[] iv = cipher.getIV();
            outputStream.write(iv); // Save IV at the start of file

            byte[] buffer = new byte[1024 * 4]; // 4KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) outputStream.write(output);
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) outputStream.write(finalBytes);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Decrypts stream. Reads IV from start of input stream.
     */
    public boolean decrypt(InputStream inputStream, OutputStream outputStream) {
        try {
            // Read IV (GCM standard is 12 bytes)
            byte[] iv = new byte[12];
            int ivRead = inputStream.read(iv);
            if (ivRead != 12) return false;

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);

            byte[] buffer = new byte[1024 * 4];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) outputStream.write(output);
            }
            byte[] finalBytes = cipher.doFinal();
            if (finalBytes != null) outputStream.write(finalBytes);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}