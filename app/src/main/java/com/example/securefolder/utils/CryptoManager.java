package com.example.securefolder.utils;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * CryptoManager
 * Handles AES-GCM Encryption for Files AND Strings.
 */
public class CryptoManager {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // Recommended for GCM
    private static final int TAG_LENGTH = 128;

    /**
     * Encrypts a String and returns a Base64 encoded string (IV + EncryptedData).
     */
    public static String encryptString(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return "";
        try {
            SecretKey key = KeyManager.getMasterKey();
            if (key == null) return null; // Key lost, cannot encrypt

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();

            byte[] inputBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(inputBytes);

            // Combine IV + Encrypted Bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(iv);
            outputStream.write(encryptedBytes);

            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Decrypts a Base64 encoded string (IV + EncryptedData) back to Plaintext.
     */
    public static String decryptString(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return "";
        try {
            SecretKey key = KeyManager.getMasterKey();
            if (key == null) return null; // Key lost

            byte[] decodedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP);

            // Extract IV
            if (decodedBytes.length < IV_LENGTH) return ""; // Invalid data
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decodedBytes, 0, iv, 0, IV_LENGTH);

            // Extract CipherText
            int encryptedSize = decodedBytes.length - IV_LENGTH;
            byte[] encryptedBytes = new byte[encryptedSize];
            System.arraycopy(decodedBytes, IV_LENGTH, encryptedBytes, 0, encryptedSize);

            // Decrypt
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return "[Decryption Failed]";
        }
    }

    /**
     * Encrypts data from inputStream and writes to outputStream.
     * Format: [IV (12 bytes)] + [Encrypted Data]
     */
    public static boolean encrypt(SecretKey key, InputStream inputStream, OutputStream outputStream) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            outputStream.write(iv);

            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }

            cipherOutputStream.close();
            inputStream.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Decrypts data from inputStream and writes to outputStream.
     */
    public static boolean decrypt(SecretKey key, InputStream inputStream, OutputStream outputStream) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            int ivRead = inputStream.read(iv);
            if (ivRead != IV_LENGTH) return false;

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            cipherInputStream.close();
            inputStream.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}