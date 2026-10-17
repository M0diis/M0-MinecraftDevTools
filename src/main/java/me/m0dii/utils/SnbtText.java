package me.m0dii.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Text helpers for SNBT that work on any input, valid or not: tokens for colouring, pretty/compact formatting,
 * bracket matching, line/column lookup and auto-indent. Understands both quote styles and escapes.
 */
public final class SnbtText {
    public enum Kind { STRING, KEY, NUMBER, BOOLEAN, PUNCTUATION, WORD, WHITESPACE }

    public record Token(int start, int end, Kind kind) {
    }

    public record LineCol(int line, int column) {
    }

    private static final String PUNCTUATION = "{}[]:,;";

    private SnbtText() {
    }

    // ---- tokens ------------------------------------------------------------------------------------------

    /** Tokens of one line (or any text). Strings that don't close run to the end. */
    public static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                int end = stringEnd(text, i);
                tokens.add(new Token(i, end, isKey(text, end) ? Kind.KEY : Kind.STRING));
                i = end;
            } else if (PUNCTUATION.indexOf(ch) >= 0) {
                tokens.add(new Token(i, i + 1, Kind.PUNCTUATION));
                i++;
            } else if (Character.isWhitespace(ch)) {
                int end = i + 1;
                while (end < n && Character.isWhitespace(text.charAt(end))) {
                    end++;
                }
                tokens.add(new Token(i, end, Kind.WHITESPACE));
                i = end;
            } else {
                int end = i + 1;
                while (end < n && !Character.isWhitespace(text.charAt(end)) && PUNCTUATION.indexOf(text.charAt(end)) < 0
                        && text.charAt(end) != '"' && text.charAt(end) != '\'') {
                    end++;
                }
                tokens.add(new Token(i, end, classifyWord(text.substring(i, end), isKey(text, end))));
                i = end;
            }
        }
        return tokens;
    }

    private static Kind classifyWord(String word, boolean key) {
        if (key) {
            return Kind.KEY;
        }
        if (word.matches("[-+]?(?:[0-9]+\\.?[0-9]*|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?[bBsSlLfFdD]?")) {
            return Kind.NUMBER;
        }
        if (word.equals("true") || word.equals("false")) {
            return Kind.BOOLEAN;
        }
        return Kind.WORD;
    }

    /** True when the next non-space character after {@code index} is a colon. */
    private static boolean isKey(String text, int index) {
        int i = index;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return i < text.length() && text.charAt(i) == ':';
    }

    /** Index just past the closing quote of the string starting at {@code start} (or the text end). */
    static int stringEnd(String text, int start) {
        char quote = text.charAt(start);
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i + 1;
            }
            i++;
        }
        return text.length();
    }

    // ---- formatting --------------------------------------------------------------------------------------

    /** Everything outside strings without whitespace: {@code {a:1b,b:[1,2]}}. */
    public static String compact(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '"' || ch == '\'') {
                int end = stringEnd(text, i);
                out.append(text, i, end);
                i = end;
                continue;
            }
            if (!Character.isWhitespace(ch)) {
                out.append(ch);
            }
            i++;
        }
        return out.toString();
    }

    /**
     * Pretty prints with one entry per line. Empty containers stay {@code {}} / {@code []}, and lists without
     * nested containers that fit in {@code inlineWidth} characters stay on one line.
     */
    public static String format(String text, String indentUnit, int inlineWidth) {
        String src = compact(text);
        if (src.isEmpty()) {
            return "{}";
        }
        StringBuilder out = new StringBuilder(src.length() * 2);
        int[] partner = partners(src);
        int depth = 0;
        int i = 0;
        while (i < src.length()) {
            char ch = src.charAt(i);
            if (ch == '"' || ch == '\'') {
                int end = stringEnd(src, i);
                out.append(src, i, end);
                i = end;
                continue;
            }
            switch (ch) {
                case '{', '[' -> {
                    int close = partner[i];
                    if (close == i + 1) {
                        out.append(ch).append(src.charAt(close));
                        i = close + 1;
                        continue;
                    }
                    if (ch == '[' && close > 0 && fitsInline(src, i, close, inlineWidth)) {
                        out.append(inlineList(src.substring(i, close + 1)));
                        i = close + 1;
                        continue;
                    }
                    depth++;
                    out.append(ch);
                    // Keep typed array prefixes like [I; on the opening line.
                    if (ch == '[' && i + 2 < src.length() && src.charAt(i + 2) == ';' && "BIL".indexOf(src.charAt(i + 1)) >= 0) {
                        out.append(src.charAt(i + 1)).append(';');
                        i += 2;
                    }
                    newline(out, indentUnit, depth);
                }
                case '}', ']' -> {
                    depth = Math.max(0, depth - 1);
                    newline(out, indentUnit, depth);
                    out.append(ch);
                }
                case ',' -> {
                    out.append(',');
                    newline(out, indentUnit, depth);
                }
                case ':' -> out.append(": ");
                default -> out.append(ch);
            }
            i++;
        }
        return out.toString();
    }

    public static String format(String text) {
        return format(text, "  ", 60);
    }

    private static boolean fitsInline(String src, int open, int close, int inlineWidth) {
        if (close - open + 1 > inlineWidth) {
            return false;
        }
        int i = open + 1;
        while (i < close) {
            char c = src.charAt(i);
            if (c == '"' || c == '\'') {
                i = stringEnd(src, i);
                continue;
            }
            if (c == '{' || c == '[') {
                return false;
            }
            i++;
        }
        return true;
    }

    private static String inlineList(String compactList) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < compactList.length()) {
            char c = compactList.charAt(i);
            if (c == '"' || c == '\'') {
                int end = stringEnd(compactList, i);
                out.append(compactList, i, end);
                i = end;
                continue;
            }
            out.append(c);
            if (c == ',' || c == ';') {
                out.append(' ');
            }
            i++;
        }
        return out.toString();
    }

    private static void newline(StringBuilder out, String indentUnit, int depth) {
        out.append('\n');
        out.append(indentUnit.repeat(Math.max(0, depth)));
    }

    // ---- navigation --------------------------------------------------------------------------------------

    /** Index of the bracket matching the one at {@code index}, or -1. Brackets inside strings are ignored. */
    public static int matchingBracket(String text, int index) {
        if (text == null || index < 0 || index >= text.length()) {
            return -1;
        }
        char ch = text.charAt(index);
        if ("{[".indexOf(ch) < 0 && "}]".indexOf(ch) < 0) {
            return -1;
        }
        return partners(text)[index];
    }

    /** For every bracket outside strings, the index of its partner (or -1). */
    private static int[] partners(String text) {
        List<Integer> stack = new ArrayList<>();
        int[] partner = new int[text.length()];
        java.util.Arrays.fill(partner, -1);
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                i = stringEnd(text, i);
                continue;
            }
            if (c == '{' || c == '[') {
                stack.add(i);
            } else if ((c == '}' || c == ']') && !stack.isEmpty()) {
                int open = stack.remove(stack.size() - 1);
                if ((text.charAt(open) == '{') == (c == '}')) {
                    partner[open] = i;
                    partner[i] = open;
                }
            }
            i++;
        }
        return partner;
    }

    /** True when {@code index} is inside a quoted string. */
    public static boolean insideString(String text, int index) {
        int i = 0;
        while (i < text.length() && i < index) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                int end = stringEnd(text, i);
                if (index < end && index > i) {
                    return true;
                }
                i = end;
                continue;
            }
            i++;
        }
        return false;
    }

    /** Zero-based line and column of {@code index}. */
    public static LineCol lineCol(String text, int index) {
        int line = 0;
        int lineStart = 0;
        int limit = Math.clamp(index, 0, text.length());
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        return new LineCol(line, limit - lineStart);
    }

    /**
     * What to insert for Enter at {@code cursor}: a newline plus the current line's indentation, one level
     * deeper after an opening bracket. Between a bracket pair the closing bracket goes to its own line.
     */
    public static String newlineInsert(String text, int cursor, String indentUnit) {
        int lineStart = cursor;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        int indentEnd = lineStart;
        while (indentEnd < cursor && (text.charAt(indentEnd) == ' ' || text.charAt(indentEnd) == '\t')) {
            indentEnd++;
        }
        String indent = text.substring(lineStart, indentEnd);
        char before = previousNonSpace(text, cursor);
        char after = cursor < text.length() ? text.charAt(cursor) : 0;
        if (before == '{' || before == '[') {
            if ((before == '{' && after == '}') || (before == '[' && after == ']')) {
                return "\n" + indent + indentUnit + "\n" + indent;
            }
            return "\n" + indent + indentUnit;
        }
        return "\n" + indent;
    }

    private static char previousNonSpace(String text, int cursor) {
        int i = cursor - 1;
        while (i >= 0 && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i--;
        }
        return i >= 0 ? text.charAt(i) : 0;
    }

    /** Start of the word before {@code cursor} (for Ctrl+Left / Ctrl+Backspace). */
    public static int wordLeft(String text, int cursor) {
        int i = Math.clamp(cursor, 0, text.length());
        while (i > 0 && !isWordChar(text.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && isWordChar(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    /** End of the word after {@code cursor} (for Ctrl+Right / Ctrl+Delete). */
    public static int wordRight(String text, int cursor) {
        int i = Math.clamp(cursor, 0, text.length());
        while (i < text.length() && !isWordChar(text.charAt(i))) {
            i++;
        }
        while (i < text.length() && isWordChar(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-' || c == '+';
    }
}
