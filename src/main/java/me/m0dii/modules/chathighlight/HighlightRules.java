package me.m0dii.modules.chathighlight;

import me.m0dii.utils.DeadlineCharSequence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Chat highlight rules and the pure matching logic: which character ranges of a message get which colour,
 * and whether a sound should play. {@code {me}} in a pattern stands for your own player name.
 */
public final class HighlightRules {
    private static final Logger LOGGER = LogManager.getLogger("m0-dev-tools");

    /** Time budget for matching all rules against one message, so a pathological regex cannot freeze the client. */
    static final long MATCH_TIME_LIMIT_NANOS = 50_000_000L; // 50 ms

    private static final java.util.Set<String> WARNED_SLOW = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static final class Rule {
        public String pattern = "";
        public String color = "gold";
        public boolean sound = false;
        public boolean regex = false;
        public boolean enabled = true;
    }

    public record Span(int start, int end, int rgb) {
    }

    public record Result(List<Span> spans, boolean sound) {
        public boolean matched() {
            return !spans.isEmpty();
        }
    }

    static final Map<String, Integer> NAMED_COLORS = Map.ofEntries(
            Map.entry("red", 0xFF5555), Map.entry("gold", 0xFFAA00), Map.entry("yellow", 0xFFFF55),
            Map.entry("green", 0x55FF55), Map.entry("aqua", 0x55FFFF), Map.entry("blue", 0x5555FF),
            Map.entry("purple", 0xFF55FF), Map.entry("pink", 0xFF8AD8), Map.entry("orange", 0xFF9933),
            Map.entry("white", 0xFFFFFF), Map.entry("gray", 0xAAAAAA));

    private HighlightRules() {
    }

    /** Parses "gold", "#ffaa00" or "ffaa00"; returns -1 if unknown. */
    public static int parseColor(String raw) {
        if (raw == null) {
            return -1;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        Integer named = NAMED_COLORS.get(value);
        if (named != null) {
            return named;
        }
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.matches("[0-9a-f]{6}")) {
            return Integer.parseInt(value, 16);
        }
        return -1;
    }

    /** Compiles a rule; plain-text rules match whole words, case-insensitively. Returns null when invalid. */
    public static Pattern compile(Rule rule, String playerName) {
        if (rule == null || rule.pattern == null || rule.pattern.isBlank()) {
            return null;
        }
        String me = playerName == null || playerName.isBlank() ? "\u0000no-player\u0000" : playerName;
        try {
            if (rule.regex) {
                return Pattern.compile(rule.pattern.replace("{me}", Pattern.quote(me)),
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            }
            String text = rule.pattern.equalsIgnoreCase("{me}") ? me : rule.pattern.replace("{me}", me);
            return Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(text) + "(?![\\p{L}\\p{N}_])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    /**
     * Finds highlighted ranges. Earlier rules win where ranges overlap. Own chat lines ({@code <Name> ...})
     * are not highlighted for {@code {me}} rules, so you don't ping yourself.
     */
    public static Result match(List<Rule> rules, String text, String playerName) {
        List<Span> spans = new ArrayList<>();
        boolean sound = false;
        if (text == null || text.isEmpty() || rules == null) {
            return new Result(spans, false);
        }
        boolean ownLine = playerName != null && !playerName.isBlank() && text.startsWith("<" + playerName + ">");
        boolean[] taken = new boolean[text.length()];
        CharSequence guarded = DeadlineCharSequence.within(text, MATCH_TIME_LIMIT_NANOS);
        for (Rule rule : rules) {
            if (rule == null || !rule.enabled) {
                continue;
            }
            if (ownLine && rule.pattern != null && rule.pattern.contains("{me}")) {
                continue;
            }
            Pattern pattern = compile(rule, playerName);
            int rgb = parseColor(rule.color);
            if (pattern == null || rgb < 0) {
                continue;
            }
            Matcher matcher = pattern.matcher(guarded);
            try {
                while (matcher.find()) {
                    if (matcher.end() <= matcher.start()) {
                        continue;
                    }
                    boolean overlap = false;
                    for (int i = matcher.start(); i < matcher.end(); i++) {
                        overlap |= taken[i];
                    }
                    if (overlap) {
                        continue;
                    }
                    for (int i = matcher.start(); i < matcher.end(); i++) {
                        taken[i] = true;
                    }
                    spans.add(new Span(matcher.start(), matcher.end(), rgb));
                    sound |= rule.sound;
                }
            } catch (DeadlineCharSequence.TimeoutException e) {
                // Out of time for this message: keep what was found so far and skip the remaining rules.
                if (WARNED_SLOW.size() < 64 && WARNED_SLOW.add(String.valueOf(rule.pattern))) {
                    LOGGER.warn("Chat highlight rule '{}' took too long to match and was skipped. "
                            + "Simplify the regex (avoid nested repeats like (a+)+).", rule.pattern);
                }
                break;
            }
        }
        spans.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return new Result(spans, sound);
    }

    /** A run of text with its original style, and the highlight colour to apply (or {@code null}). */
    public record Segment<S>(S style, String text, Integer rgb) {
    }

    /**
     * Splits styled parts (as they come from walking a chat component) at highlight boundaries, so every
     * original style is kept and only highlighted runs get a colour. {@code spans} must be sorted and
     * non-overlapping, as returned by {@link #match}.
     */
    public static <S> List<Segment<S>> split(List<Map.Entry<S, String>> parts, List<Span> spans) {
        List<Segment<S>> out = new ArrayList<>();
        int position = 0;
        int spanIndex = 0;
        for (Map.Entry<S, String> part : parts) {
            String text = part.getValue() == null ? "" : part.getValue();
            int partStart = position;
            int partEnd = position + text.length();
            int cursor = partStart;
            while (cursor < partEnd) {
                while (spanIndex < spans.size() && spans.get(spanIndex).end() <= cursor) {
                    spanIndex++;
                }
                Span span = spanIndex < spans.size() ? spans.get(spanIndex) : null;
                if (span == null || span.start() >= partEnd) {
                    out.add(new Segment<>(part.getKey(), text.substring(cursor - partStart), null));
                    cursor = partEnd;
                } else if (span.start() > cursor) {
                    out.add(new Segment<>(part.getKey(), text.substring(cursor - partStart, span.start() - partStart), null));
                    cursor = span.start();
                } else {
                    int end = Math.min(span.end(), partEnd);
                    out.add(new Segment<>(part.getKey(), text.substring(cursor - partStart, end - partStart), span.rgb()));
                    cursor = end;
                }
            }
            position = partEnd;
        }
        return out;
    }
}
