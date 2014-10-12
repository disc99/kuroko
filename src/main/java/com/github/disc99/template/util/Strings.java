package com.github.disc99.template.util;

/**
 * String utilities
 */
public final class Strings {

    public static String capitalize(String value) {
        if (isNullOrEmpty(value)) {
            return "";
        }
        int length = value.length();
        return new StringBuilder(length).append(Character.toTitleCase(value.charAt(0))).append(value.substring(1)).toString();
    }

    public static String uncapitalize(String value) {
        if (isNullOrEmpty(value)) {
            return "";
        }
        int length = value.length();
        return new StringBuilder(length).append(Character.toLowerCase(value.charAt(0))).append(value.substring(1)).toString();
    }

    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

    private Strings() {
    }
}
