package me.m0dii.utils;

public final class CpsTracker {
    private static final int WINDOW_SECONDS = 4;
    private static final int[][] BUCKETS = new int[2][WINDOW_SECONDS];
    private static long lastSecond = -1L;

    private CpsTracker() {
    }

    public static synchronized void registerClick(int button) {
        int index = button == 1 ? 1 : 0;
        if (button != 0 && button != 1) {
            return;
        }
        rollWindow();
        BUCKETS[index][0]++;
    }

    public static synchronized int getLeftCps() {
        rollWindow();
        return BUCKETS[0][0];
    }

    public static synchronized int getRightCps() {
        rollWindow();
        return BUCKETS[1][0];
    }

    public static synchronized int getTotalCps() {
        return getLeftCps() + getRightCps();
    }

    private static void rollWindow() {
        long sec = System.currentTimeMillis() / 1000L;
        if (lastSecond < 0L) {
            lastSecond = sec;
            return;
        }
        long delta = sec - lastSecond;
        if (delta <= 0L) {
            return;
        }
        int shift = (int) Math.min(delta, WINDOW_SECONDS);
        for (int b = 0; b < BUCKETS.length; b++) {
            for (int i = WINDOW_SECONDS - 1; i >= 0; i--) {
                int from = i - shift;
                BUCKETS[b][i] = from >= 0 ? BUCKETS[b][from] : 0;
            }
        }
        lastSecond = sec;
    }
}

