package com.money.mimi.telemetry;

import java.util.Locale;

/** Keeps telemetry identifiers bounded and free of user-provided values. */
final class TelemetrySanitizer {
    private static final int MAX_NAME_LENGTH = 40;

    private TelemetrySanitizer() { }

    static String identifier(String value, String fallback) {
        String safeFallback = normalize(fallback);
        if (safeFallback.isEmpty()) safeFallback = "unknown";

        String normalized = normalize(value);
        return normalized.isEmpty() ? safeFallback : normalized;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String lower = value.trim().toLowerCase(Locale.US);
        StringBuilder result = new StringBuilder(Math.min(lower.length(), MAX_NAME_LENGTH));
        boolean previousUnderscore = false;
        for (int i = 0; i < lower.length() && result.length() < MAX_NAME_LENGTH; i++) {
            char character = lower.charAt(i);
            boolean allowed = character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9';
            if (allowed) {
                result.append(character);
                previousUnderscore = false;
            } else if (!previousUnderscore && result.length() > 0) {
                result.append('_');
                previousUnderscore = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '_') {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }
}
