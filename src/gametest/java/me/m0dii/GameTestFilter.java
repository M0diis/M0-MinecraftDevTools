package me.m0dii;

import java.util.Arrays;
import java.util.Locale;

/**
 * Lets a run pick which client game tests to execute: {@code M0DEV_GAMETESTS=features} (comma-separated names).
 * Unset runs everything.
 */
final class GameTestFilter {
    private GameTestFilter() {
    }

    static boolean enabled(String name) {
        String filter = System.getenv("M0DEV_GAMETESTS");
        if (filter == null || filter.isBlank()) {
            return true;
        }
        return Arrays.stream(filter.toLowerCase(Locale.ROOT).split(","))
                .map(String::trim)
                .anyMatch(name.toLowerCase(Locale.ROOT)::equals);
    }
}
