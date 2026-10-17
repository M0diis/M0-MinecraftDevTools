package me.m0dii.modules.scripting.ide;

/**
 * Editor commands that work on whole lines or the text around the cursor. Every method takes the text and the
 * selection ({@code anchor} is -1 when nothing is selected) and returns the new text and selection.
 */
public final class LineOps {
    public record Edit(String text, int cursor, int anchor) {
    }

    private LineOps() {
    }

    public static int lineStart(String text, int index) {
        int i = Math.clamp(index, 0, text.length());
        while (i > 0 && text.charAt(i - 1) != '\n') {
            i--;
        }
        return i;
    }

    public static int lineEnd(String text, int index) {
        int i = Math.clamp(index, 0, text.length());
        while (i < text.length() && text.charAt(i) != '\n') {
            i++;
        }
        return i;
    }

    /** Start of the first and end of the last line touched by the selection (or the cursor's line). */
    private static int[] block(String text, int cursor, int anchor) {
        int from = anchor < 0 ? cursor : Math.min(cursor, anchor);
        int to = anchor < 0 ? cursor : Math.max(cursor, anchor);
        // A selection ending at the very start of a line doesn't include that line.
        if (anchor >= 0 && to > from && to > 0 && text.charAt(to - 1) == '\n') {
            to--;
        }
        return new int[]{lineStart(text, from), lineEnd(text, to)};
    }

    /** Ctrl+/: comments out the lines with {@code prefix}, or removes it when every non-blank line has it. */
    public static Edit toggleComment(String text, int cursor, int anchor, String prefix) {
        int[] b = block(text, cursor, anchor);
        String[] lines = text.substring(b[0], b[1]).split("\n", -1);
        boolean allCommented = true;
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            int indent = indentOf(line);
            minIndent = Math.min(minIndent, indent);
            if (!line.startsWith(prefix, indent)) {
                allCommented = false;
            }
        }
        if (minIndent == Integer.MAX_VALUE) {
            minIndent = 0;
            allCommented = false;
        }
        StringBuilder out = new StringBuilder();
        int delta = 0;
        int cursorLineDelta = 0;
        int cursorLine = countNewlines(text, b[0], Math.clamp(cursor, b[0], b[1]));
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String changed;
            if (line.isBlank()) {
                changed = line;
            } else if (allCommented) {
                int indent = indentOf(line);
                int cut = prefix.length() + (line.startsWith(prefix + " ", indent) ? 1 : 0);
                changed = line.substring(0, indent) + line.substring(indent + cut);
            } else {
                changed = line.substring(0, minIndent) + prefix + " " + line.substring(minIndent);
            }
            if (i == cursorLine) {
                cursorLineDelta = changed.length() - line.length();
            }
            delta += changed.length() - line.length();
            out.append(changed);
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        String result = text.substring(0, b[0]) + out + text.substring(b[1]);
        if (anchor < 0) {
            int lineStartOfCursor = lineStart(text, cursor);
            int newCursor = Math.max(lineStartOfCursor, cursor + cursorLineDelta);
            return new Edit(result, Math.clamp(newCursor, 0, result.length()), -1);
        }
        return new Edit(result, b[1] + delta, b[0]);
    }

    /** Ctrl+D: duplicates the selection, or the current line(s) below themselves. */
    public static Edit duplicate(String text, int cursor, int anchor) {
        if (anchor >= 0 && anchor != cursor) {
            int from = Math.min(cursor, anchor);
            int to = Math.max(cursor, anchor);
            String piece = text.substring(from, to);
            String result = text.substring(0, to) + piece + text.substring(to);
            return new Edit(result, to + piece.length(), to);
        }
        int[] b = block(text, cursor, -1);
        String piece = text.substring(b[0], b[1]);
        String result = text.substring(0, b[1]) + "\n" + piece + text.substring(b[1]);
        return new Edit(result, cursor + piece.length() + 1, -1);
    }

    /** Alt+Up / Alt+Down: moves the selected lines one line up (-1) or down (+1). */
    public static Edit moveLines(String text, int cursor, int anchor, int direction) {
        int[] b = block(text, cursor, anchor);
        if (direction < 0) {
            if (b[0] == 0) {
                return new Edit(text, cursor, anchor);
            }
            int prevStart = lineStart(text, b[0] - 1);
            String prev = text.substring(prevStart, b[0] - 1);
            String moving = text.substring(b[0], b[1]);
            String result = text.substring(0, prevStart) + moving + "\n" + prev + text.substring(b[1]);
            int shift = -(prev.length() + 1);
            return new Edit(result, cursor + shift, anchor < 0 ? -1 : anchor + shift);
        }
        if (b[1] >= text.length()) {
            return new Edit(text, cursor, anchor);
        }
        int nextEnd = lineEnd(text, b[1] + 1);
        String next = text.substring(b[1] + 1, nextEnd);
        String moving = text.substring(b[0], b[1]);
        String result = text.substring(0, b[0]) + next + "\n" + moving + text.substring(nextEnd);
        int shift = next.length() + 1;
        return new Edit(result, cursor + shift, anchor < 0 ? -1 : anchor + shift);
    }

    /** Ctrl+Shift+K: removes the current line(s). */
    public static Edit deleteLines(String text, int cursor, int anchor) {
        int[] b = block(text, cursor, anchor);
        int from = b[0];
        int to = b[1];
        if (to < text.length()) {
            to++; // take the newline after
        } else if (from > 0) {
            from--; // last line: take the newline before
        }
        String result = text.substring(0, from) + text.substring(to);
        int caret = Math.min(from == b[0] ? from : lineStart(result, from), result.length());
        return new Edit(result, caret, -1);
    }

    /** Home: jumps to the first non-space character, or to column 0 when already there. */
    public static int smartHome(String text, int cursor) {
        int start = lineStart(text, cursor);
        int firstCode = start;
        while (firstCode < text.length() && (text.charAt(firstCode) == ' ' || text.charAt(firstCode) == '\t')) {
            firstCode++;
        }
        return cursor == firstCode ? start : firstCode;
    }

    public static int wordLeft(String text, int cursor) {
        int i = Math.clamp(cursor, 0, text.length());
        while (i > 0 && Character.isWhitespace(text.charAt(i - 1)) && text.charAt(i - 1) != '\n') {
            i--;
        }
        if (i > 0 && Character.isJavaIdentifierPart(text.charAt(i - 1))) {
            while (i > 0 && Character.isJavaIdentifierPart(text.charAt(i - 1))) {
                i--;
            }
        } else if (i > 0) {
            i--;
        }
        return i;
    }

    public static int wordRight(String text, int cursor) {
        int i = Math.clamp(cursor, 0, text.length());
        if (i < text.length() && Character.isJavaIdentifierPart(text.charAt(i))) {
            while (i < text.length() && Character.isJavaIdentifierPart(text.charAt(i))) {
                i++;
            }
        } else if (i < text.length()) {
            i++;
        }
        while (i < text.length() && Character.isWhitespace(text.charAt(i)) && text.charAt(i) != '\n') {
            i++;
        }
        return i;
    }

    /** The closing character to add after typing {@code open}, or 0 when it shouldn't be paired here. */
    public static char autoClose(String text, int cursor, char open) {
        char close = switch (open) {
            case '(' -> ')';
            case '[' -> ']';
            case '{' -> '}';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> 0;
        };
        if (close == 0) {
            return 0;
        }
        char next = cursor < text.length() ? text.charAt(cursor) : 0;
        boolean freeAfter = next == 0 || Character.isWhitespace(next) || ")]},;".indexOf(next) >= 0;
        if (!freeAfter) {
            return 0;
        }
        if (open == '"' || open == '\'') {
            char prev = cursor > 0 ? text.charAt(cursor - 1) : 0;
            // Don't pair right after a letter (it's likely an apostrophe or closing quote).
            if (Character.isLetterOrDigit(prev) || prev == open) {
                return 0;
            }
        }
        return close;
    }

    /** Whether typing {@code ch} should just step over the same character already at the cursor. */
    public static boolean skipsClosing(String text, int cursor, char ch) {
        return cursor < text.length() && text.charAt(cursor) == ch && ")]}\"'".indexOf(ch) >= 0;
    }

    /** Whether Backspace at {@code cursor} sits between an empty pair like {@code ()} and should delete both. */
    public static boolean betweenEmptyPair(String text, int cursor) {
        if (cursor <= 0 || cursor >= text.length()) {
            return false;
        }
        String pair = "" + text.charAt(cursor - 1) + text.charAt(cursor);
        return pair.equals("()") || pair.equals("[]") || pair.equals("{}") || pair.equals("\"\"") || pair.equals("''");
    }

    /** Line comment prefix for a script language. */
    public static String commentPrefix(String language) {
        return "//";
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    private static int countNewlines(String text, int from, int to) {
        int count = 0;
        for (int i = from; i < to; i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
