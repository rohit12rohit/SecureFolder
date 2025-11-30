package com.example.securefolder.utils;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * SecurityUtils
 * Handles Password Hashing, Validation, and Random Code Generation.
 */
public class SecurityUtils {

    // Password Policies
    public static final int POLICY_SIMPLE = 0;
    public static final int POLICY_RECOMMENDED = 1;
    public static final int POLICY_STRONG = 2;

    /**
     * Hashes a password using SHA-256 with a salt.
     */
    public static String hashPassword(String password, String salt) {
        try {
            String input = password + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a random Salt (random text) to make hashes unique.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    /**
     * Generates a 32-character recovery code (A-Z, 0-9).
     */
    public static String generateRecoveryCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 32; i++) {
            if (i > 0 && i % 8 == 0) sb.append("-"); // Add hyphens for readability
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Checks if password meets the chosen policy requirements.
     */
    public static boolean isValidPassword(String password, int policy) {
        if (password == null) return false;

        switch (policy) {
            case POLICY_SIMPLE:
                // Min 6 chars
                return password.length() >= 6;

            case POLICY_RECOMMENDED:
                // Min 8 chars, 1 Upper, 1 Lower, 1 Digit
                return password.length() >= 8 &&
                        password.matches(".*[A-Z].*") &&
                        password.matches(".*[a-z].*") &&
                        password.matches(".*\\d.*");

            case POLICY_STRONG:
                // Min 12 chars, 1 Upper, 1 Lower, 1 Digit, 1 Special
                return password.length() >= 12 &&
                        password.matches(".*[A-Z].*") &&
                        password.matches(".*[a-z].*") &&
                        password.matches(".*\\d.*") &&
                        password.matches(".*[!@#$%^&*()_+=].*");

            default:
                return false;
        }
    }
}