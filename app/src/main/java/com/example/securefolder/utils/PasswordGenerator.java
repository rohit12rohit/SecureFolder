package com.example.securefolder.utils;

import java.security.SecureRandom;

public class PasswordGenerator {

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String PUNCTUATION = "!@#$%&*()_+-=[]?";

    public static String generate(boolean useUpper, boolean useDigits, boolean useSymbols, int length) {
        StringBuilder charSet = new StringBuilder(LOWER);
        if (useUpper) charSet.append(UPPER);
        if (useDigits) charSet.append(DIGITS);
        if (useSymbols) charSet.append(PUNCTUATION);

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charSet.length());
            password.append(charSet.charAt(index));
        }

        return password.toString();
    }
}