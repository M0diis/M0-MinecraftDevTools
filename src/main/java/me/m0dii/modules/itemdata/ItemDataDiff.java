package me.m0dii.modules.itemdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Human-readable difference between two flattened item snapshots (key → SNBT text), shown before saving.
 * Keys are root fields like {@code id} / {@code count} and {@code components/<component id>}.
 */
public final class ItemDataDiff {
    private static final int MAX_VALUE_CHARS = 90;

    private ItemDataDiff() {
    }

    public static List<String> diff(Map<String, String> before, Map<String, String> after) {
        List<String> lines = new ArrayList<>();
        TreeSet<String> keys = new TreeSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (String key : keys) {
            String old = before.get(key);
            String now = after.get(key);
            if (old == null && now != null) {
                lines.add("+ " + key + " = " + shorten(now));
            } else if (old != null && now == null) {
                lines.add("- " + key);
            } else if (old != null && !old.equals(now)) {
                lines.add("~ " + key + ": " + shorten(old) + " → " + shorten(now));
            }
        }
        return lines;
    }

    static String shorten(String value) {
        String flat = value.replace('\n', ' ');
        return flat.length() > MAX_VALUE_CHARS ? flat.substring(0, MAX_VALUE_CHARS) + "…" : flat;
    }
}
