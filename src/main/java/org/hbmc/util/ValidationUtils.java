package org.hbmc.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_-]+(\\.[A-Za-z0-9+_-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");

    private ValidationUtils() {}

    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return isNotBlank(password) && password.length() >= 6;
    }

    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidFullName(String fullName) {
        return isNotBlank(fullName);
    }

    public static void validatePositiveGuestCount(int numberOfGuests) {
        if (numberOfGuests <= 0) throw new IllegalArgumentException("Number of guests must be positive");
    }
}