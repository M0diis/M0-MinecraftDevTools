package me.m0dii.modules.worldedit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Formatting for the materials list: stack/shulker counts, sorting and CSV. */
public final class MaterialsMath {
    public static final int STACK = 64;
    public static final int SHULKER = 27 * STACK;

    private MaterialsMath() {
    }

    /** "3 stacks + 12", "1 shulker + 5 stacks", "40" … based on 64-item stacks. */
    public static String stacks(long count) {
        if (count < STACK) {
            return Long.toString(count);
        }
        long shulkers = count / SHULKER;
        long rest = count % SHULKER;
        long stacks = rest / STACK;
        long items = rest % STACK;
        List<String> parts = new ArrayList<>();
        if (shulkers > 0) {
            parts.add(shulkers + (shulkers == 1 ? " shulker" : " shulkers"));
        }
        if (stacks > 0) {
            parts.add(stacks + (stacks == 1 ? " stack" : " stacks"));
        }
        if (items > 0) {
            parts.add(Long.toString(items));
        }
        return String.join(" + ", parts);
    }

    /** Entries sorted by count (highest first), then name. */
    public static <K> List<Map.Entry<K, Long>> sorted(Map<K, Long> counts, java.util.function.Function<K, String> nameOf) {
        List<Map.Entry<K, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> {
            int byCount = Long.compare(b.getValue(), a.getValue());
            return byCount != 0 ? byCount : nameOf.apply(a.getKey()).compareToIgnoreCase(nameOf.apply(b.getKey()));
        });
        return entries;
    }

    public static <K> String csv(List<Map.Entry<K, Long>> sorted, java.util.function.Function<K, String> idOf,
                                 java.util.function.Function<K, String> nameOf) {
        StringBuilder out = new StringBuilder("block,name,count,stacks\n");
        for (Map.Entry<K, Long> entry : sorted) {
            out.append(idOf.apply(entry.getKey())).append(',')
                    .append('"').append(nameOf.apply(entry.getKey()).replace("\"", "\"\"")).append('"').append(',')
                    .append(entry.getValue()).append(',')
                    .append(String.format(Locale.ROOT, "%.2f", entry.getValue() / (double) STACK)).append('\n');
        }
        return out.toString();
    }
}
