package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex support for automation filters and conditions.
 * <ul>
 *     <li>Patterns are compiled once and cached; invalid patterns never throw, they just never match.</li>
 *     <li>Matching uses {@link Matcher#find()} (same as chat tab routing), so {@code joined the game} matches anywhere
 *     in the text. Anchor with {@code ^...$} for a full match.</li>
 *     <li>Matching is time-limited so a pathological pattern cannot freeze the client.</li>
 *     <li>Capture groups are returned as {@code group0..groupN} plus {@code group.<name>} for named groups.</li>
 * </ul>
 */
public final class AutomationRegex {
    static final long MATCH_TIME_LIMIT_NANOS = 50_000_000L; // 50 ms
    private static final int CACHE_LIMIT = 256;
    private static final Pattern INVALID = Pattern.compile("(?!)");

    private static final Map<String, Pattern> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
            return size() > CACHE_LIMIT;
        }
    });

    private AutomationRegex() {
    }

    /** Result of a match attempt. {@code groups} is empty when the pattern did not match. */
    public record Result(boolean matched, @NotNull Map<String, Object> groups, @Nullable String error) {
        static final Result NO_MATCH = new Result(false, Map.of(), null);
    }

    public static @NotNull Result match(@Nullable String regex, @Nullable String input, boolean ignoreCase) {
        if (regex == null) {
            return Result.NO_MATCH;
        }
        Pattern pattern = compile(regex, ignoreCase);
        if (pattern == INVALID) {
            return new Result(false, Map.of(), "Invalid regex: " + regex);
        }

        String text = input == null ? "" : input;
        try {
            Matcher matcher = pattern.matcher(new DeadlineCharSequence(text, System.nanoTime() + MATCH_TIME_LIMIT_NANOS));
            if (!matcher.find()) {
                return Result.NO_MATCH;
            }
            Map<String, Object> groups = new LinkedHashMap<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                String value = matcher.group(i);
                groups.put("group" + i, value == null ? "" : value);
            }
            for (Map.Entry<String, Integer> named : matcher.namedGroups().entrySet()) {
                String value = matcher.group(named.getValue());
                groups.put("group." + named.getKey(), value == null ? "" : value);
            }
            return new Result(true, groups, null);
        } catch (DeadlineCharSequence.TimeoutException e) {
            return new Result(false, Map.of(), "Regex timed out: " + regex);
        }
    }

    static @NotNull Pattern compile(@NotNull String regex, boolean ignoreCase) {
        String key = (ignoreCase ? "i:" : "s:") + regex;
        Pattern cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        Pattern compiled;
        try {
            compiled = Pattern.compile(regex, ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
        } catch (PatternSyntaxException e) {
            compiled = INVALID;
        }
        CACHE.put(key, compiled);
        return compiled;
    }

    /**
     * CharSequence that aborts matching once a deadline passes. Regex backtracking reads characters constantly,
     * so checking the clock in {@link #charAt(int)} bounds the total match time.
     */
    static final class DeadlineCharSequence implements CharSequence {
        static final class TimeoutException extends RuntimeException {
            TimeoutException() {
                super(null, null, false, false);
            }
        }

        private final CharSequence delegate;
        private final long deadlineNanos;
        private int reads;

        DeadlineCharSequence(CharSequence delegate, long deadlineNanos) {
            this.delegate = delegate;
            this.deadlineNanos = deadlineNanos;
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
}
