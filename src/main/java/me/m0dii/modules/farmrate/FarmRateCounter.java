package me.m0dii.modules.farmrate;

import java.util.*;

/** Item totals and per-hour rates for a farm: the average since the start and over the last few minutes. */
public final class FarmRateCounter {
    public static final long RECENT_WINDOW_MS = 5 * 60_000L;
    /** Rates are not shown before this much time has passed; they would be wild guesses. */
    public static final long MIN_ELAPSED_MS = 20_000L;

    public record Row(String item, long total, double perHour, double recentPerHour) {
    }

    private record Sample(long timeMs, String item, long count) {
    }

    private final Map<String, Long> totals = new HashMap<>();
    private final Deque<Sample> recent = new ArrayDeque<>();
    private long startMs = -1;

    public void start(long nowMs) {
        totals.clear();
        recent.clear();
        startMs = nowMs;
    }

    public boolean isStarted() {
        return startMs >= 0;
    }

    public void add(String item, long count, long nowMs) {
        if (count <= 0 || startMs < 0) {
            return;
        }
        totals.merge(item, count, Long::sum);
        recent.addLast(new Sample(nowMs, item, count));
        trim(nowMs);
    }

    public long elapsedMs(long nowMs) {
        return startMs < 0 ? 0 : Math.max(0, nowMs - startMs);
    }

    public long total() {
        long sum = 0;
        for (long value : totals.values()) {
            sum += value;
        }
        return sum;
    }

    /** Rows sorted by total, highest first. Rates are NaN until {@link #MIN_ELAPSED_MS} has passed. */
    public List<Row> rows(long nowMs) {
        trim(nowMs);
        long elapsed = elapsedMs(nowMs);
        Map<String, Long> recentTotals = new HashMap<>();
        for (Sample sample : recent) {
            recentTotals.merge(sample.item(), sample.count(), Long::sum);
        }
        long recentSpan = Math.min(elapsed, RECENT_WINDOW_MS);
        List<Row> rows = new ArrayList<>();
        totals.forEach((item, total) -> {
            double perHour = elapsed < MIN_ELAPSED_MS ? Double.NaN : total * 3_600_000.0 / elapsed;
            double recentPerHour = elapsed < MIN_ELAPSED_MS ? Double.NaN
                    : recentTotals.getOrDefault(item, 0L) * 3_600_000.0 / recentSpan;
            rows.add(new Row(item, total, perHour, recentPerHour));
        });
        rows.sort((a, b) -> a.total() != b.total() ? Long.compare(b.total(), a.total()) : a.item().compareTo(b.item()));
        return rows;
    }

    public String csv(long nowMs) {
        StringBuilder out = new StringBuilder("item,total,per_hour,last_5_min_per_hour\n");
        for (Row row : rows(nowMs)) {
            out.append(row.item()).append(',').append(row.total()).append(',')
                    .append(formatRate(row.perHour())).append(',').append(formatRate(row.recentPerHour())).append('\n');
        }
        return out.toString();
    }

    /** "1.2k", "350", "—". */
    public static String formatRate(double perHour) {
        if (Double.isNaN(perHour)) {
            return "—";
        }
        if (perHour >= 10_000) {
            return String.format(Locale.ROOT, "%.1fk", perHour / 1000.0);
        }
        return Long.toString(Math.round(perHour));
    }

    /** "1:05:09" or "4:07". */
    public static String formatElapsed(long ms) {
        long seconds = ms / 1000;
        long h = seconds / 3600;
        long m = seconds % 3600 / 60;
        long s = seconds % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    private void trim(long nowMs) {
        while (!recent.isEmpty() && recent.peekFirst().timeMs() <= nowMs - RECENT_WINDOW_MS) {
            recent.removeFirst();
        }
    }
}
