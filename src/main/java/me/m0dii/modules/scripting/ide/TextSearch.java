package me.m0dii.modules.scripting.ide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plain-text find and replace for the script editor. */
public final class TextSearch {
    public record Match(int start, int end) {
    }

    public record Replaced(String text, int count) {
    }

    private TextSearch() {
    }

    /** All non-overlapping matches of {@code query}. Empty query = no matches. */
    public static List<Match> findAll(String text, String query, boolean caseSensitive, boolean wholeWord) {
        List<Match> matches = new ArrayList<>();
        if (text == null || query == null || query.isEmpty()) {
            return matches;
        }
        String haystack = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        String needle = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
        int from = 0;
        while (from <= haystack.length() - needle.length()) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                break;
            }
            int end = at + needle.length();
            if (!wholeWord || (isBoundary(text, at - 1) && isBoundary(text, end))) {
                matches.add(new Match(at, end));
                from = end;
            } else {
                from = at + 1;
            }
        }
        return matches;
    }

    private static boolean isBoundary(String text, int index) {
        return index < 0 || index >= text.length() || !Character.isJavaIdentifierPart(text.charAt(index));
    }

    /**
     * Index of the match to go to from {@code cursor}: the first one starting at or after it (or, backwards, the
     * last one ending before it), wrapping around. -1 when there are no matches.
     */
    public static int nextIndex(List<Match> matches, int cursor, boolean backwards) {
        if (matches.isEmpty()) {
            return -1;
        }
        if (backwards) {
            for (int i = matches.size() - 1; i >= 0; i--) {
                if (matches.get(i).end() < cursor || (matches.get(i).start() < cursor && matches.get(i).end() <= cursor)) {
                    return i;
                }
            }
            return matches.size() - 1;
        }
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).start() >= cursor) {
                return i;
            }
        }
        return 0;
    }

    /** Which match (0-based) contains or starts at {@code cursor}, or -1. */
    public static int indexAt(List<Match> matches, int start, int end) {
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).start() == start && matches.get(i).end() == end) {
                return i;
            }
        }
        return -1;
    }

    public static Replaced replaceAll(String text, String query, String replacement, boolean caseSensitive, boolean wholeWord) {
        List<Match> matches = findAll(text, query, caseSensitive, wholeWord);
        if (matches.isEmpty()) {
            return new Replaced(text, 0);
        }
        StringBuilder out = new StringBuilder(text.length());
        int last = 0;
        for (Match match : matches) {
            out.append(text, last, match.start()).append(replacement);
            last = match.end();
        }
        out.append(text, last, text.length());
        return new Replaced(out.toString(), matches.size());
    }
}
