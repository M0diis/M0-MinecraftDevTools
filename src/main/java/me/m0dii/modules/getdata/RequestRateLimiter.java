package me.m0dii.modules.getdata;

/**
 * Fixed-window request limiter. Not thread-safe; callers use it from the server thread only.
 */
final class RequestRateLimiter {
    private final int maxRequests;
    private final long windowMs;
    private long windowStartMs = Long.MIN_VALUE;
    private int count;

    RequestRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowMs = Math.max(1L, windowMs);
    }

    boolean tryAcquire(long nowMs) {
        if (windowStartMs == Long.MIN_VALUE || nowMs - windowStartMs >= windowMs || nowMs < windowStartMs) {
            windowStartMs = nowMs;
            count = 0;
        }
        if (count >= maxRequests) {
            return false;
        }
        count++;
        return true;
    }
}
