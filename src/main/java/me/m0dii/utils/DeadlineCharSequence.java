package me.m0dii.utils;

import org.jetbrains.annotations.NotNull;

/**
 * CharSequence that aborts regex matching once a deadline passes. Regex backtracking reads characters constantly,
 * so checking the clock in {@link #charAt(int)} bounds the total match time and a pathological user pattern cannot
 * freeze the client.
 *
 * <p>Wrap the text, hand it to {@link java.util.regex.Pattern#matcher(CharSequence)} and catch
 * {@link TimeoutException}.
 */
public final class DeadlineCharSequence implements CharSequence {
    /** Thrown from {@link #charAt(int)} when the deadline has passed. Carries no stack trace. */
    public static final class TimeoutException extends RuntimeException {
        public TimeoutException() {
            super(null, null, false, false);
        }
    }

    private final CharSequence delegate;
    private final long deadlineNanos;
    private int reads;

    /**
     * @param deadlineNanos absolute {@link System#nanoTime()} value after which matching is aborted
     */
    public DeadlineCharSequence(CharSequence delegate, long deadlineNanos) {
        this.delegate = delegate;
        this.deadlineNanos = deadlineNanos;
    }

    /** Convenience for a deadline {@code limitNanos} from now. */
    public static DeadlineCharSequence within(CharSequence delegate, long limitNanos) {
        return new DeadlineCharSequence(delegate, System.nanoTime() + limitNanos);
    }

    @Override
    public char charAt(int index) {
        // Checking the clock on every read is wasteful; every 1024 reads is plenty.
        if ((++reads & 1023) == 0 && System.nanoTime() > deadlineNanos) {
            throw new TimeoutException();
        }
        return delegate.charAt(index);
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new DeadlineCharSequence(delegate.subSequence(start, end), deadlineNanos);
    }

    @Override
    public @NotNull String toString() {
        return delegate.toString();
    }
}
