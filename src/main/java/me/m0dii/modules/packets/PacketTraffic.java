package me.m0dii.modules.packets;

/**
 * Per-second history (last {@link #SECONDS} seconds) of one counter, e.g. bytes received or packets of a pinned
 * type. Seconds with nothing recorded read as 0.
 */
public final class PacketTraffic {
    public static final int SECONDS = 60;

    private final long[] buckets = new long[SECONDS];
    private long newestSecond = Long.MIN_VALUE;
    private long total;

    public synchronized void add(long amount, long timeMs) {
        long second = Math.floorDiv(timeMs, 1000L);
        if (newestSecond != Long.MIN_VALUE && second <= newestSecond - SECONDS) {
            return; // older than the window
        }
        advanceTo(second);
        buckets[index(second)] += amount;
        total += amount;
    }

    /** Oldest first; the last element is the current second. */
    public synchronized long[] history(long nowMs) {
        advanceTo(Math.floorDiv(nowMs, 1000L));
        long[] out = new long[SECONDS];
        for (int i = 0; i < SECONDS; i++) {
            out[i] = buckets[index(newestSecond - (SECONDS - 1) + i)];
        }
        return out;
    }

    /** Average per second over the last {@code seconds} full seconds (the running second is left out). */
    public synchronized double average(long nowMs, int seconds) {
        long[] h = history(nowMs);
        int n = Math.clamp(seconds, 1, SECONDS - 1);
        long sum = 0;
        for (int i = SECONDS - 1 - n; i < SECONDS - 1; i++) {
            sum += h[i];
        }
        return sum / (double) n;
    }

    public synchronized long total() {
        return total;
    }

    public synchronized void clear() {
        java.util.Arrays.fill(buckets, 0);
        newestSecond = Long.MIN_VALUE;
        total = 0;
    }

    private void advanceTo(long second) {
        if (newestSecond == Long.MIN_VALUE) {
            newestSecond = second;
            return;
        }
        if (second <= newestSecond) {
            return;
        }
        long gap = Math.min(second - newestSecond, SECONDS);
        for (long s = 1; s <= gap; s++) {
            buckets[index(newestSecond + s)] = 0;
        }
        newestSecond = second;
    }

    private static int index(long second) {
        return (int) Math.floorMod(second, (long) SECONDS);
    }

    /** "512 B", "3.4 KB", "1.2 MB". */
    public static String bytes(double value) {
        if (value < 1024) {
            return Math.round(value) + " B";
        }
        if (value < 1024 * 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", value / 1024);
        }
        return String.format(java.util.Locale.ROOT, "%.2f MB", value / (1024 * 1024));
    }
}
