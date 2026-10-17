package me.m0dii.modules.getdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestRateLimiterTest {
    @Test
    void allowsUpToLimitWithinWindow() {
        RequestRateLimiter limiter = new RequestRateLimiter(3, 1000L);
        assertTrue(limiter.tryAcquire(0L));
        assertTrue(limiter.tryAcquire(10L));
        assertTrue(limiter.tryAcquire(20L));
        assertFalse(limiter.tryAcquire(30L));
    }

    @Test
    void resetsAfterWindow() {
        RequestRateLimiter limiter = new RequestRateLimiter(1, 1000L);
        assertTrue(limiter.tryAcquire(0L));
        assertFalse(limiter.tryAcquire(999L));
        assertTrue(limiter.tryAcquire(1000L));
    }

    @Test
    void resetsWhenClockGoesBackwards() {
        RequestRateLimiter limiter = new RequestRateLimiter(1, 1000L);
        assertTrue(limiter.tryAcquire(5000L));
        assertTrue(limiter.tryAcquire(100L));
    }
}
