package me.m0dii.modules.scripting.ide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Syntax colouring for Kotlin script, Groovy and JavaScript. Works line by line but carries state across lines
 * for block comments, triple-quoted strings and JS template strings, so a {@code /* ... * /} spanning ten lines is
 * coloured on all of them.
 */
public final class ScriptSyntax {
    public enum Kind { PLAIN, KEYWORD, TYPE, STRING, NUMBER, COMMENT, ANNOTATION, FUNCTION, BUILTIN, PUNCTUATION }

    /** What is still open at the end of a line. */
    public enum State { CODE, BLOCK_COMMENT, TRIPLE_STRING, TEMPLATE_STRING }

    public record Span(int start, int end, Kind kind) {
    }

    public record LineResult(List<Span> spans, State endState) {
    }

    private static final Set<String> KOTLIN = Set.of("as", "break", "by", "catch", "class", "companion", "const",
            "continue", "data", "do", "else", "enum", "false", "finally", "for", "fun", "if", "import", "in", "inline",
            "interface", "internal", "is", "it", "lateinit", "null", "object", "open", "override", "package", "private",
            "protected", "public", "return", "sealed", "super", "suspend", "this", "throw", "true", "try", "typealias",
            "val", "var", "vararg", "when", "while");
    private static final Set<String> GROOVY = Set.of("as", "assert", "boolean", "break", "case", "catch", "class",
            "continue", "def", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float",
            "for", "if", "implements", "import", "in", "instanceof", "int", "interface", "it", "long", "new", "null",
            "package", "private", "protected", "public", "return", "static", "super", "switch", "this", "throw", "true",
            "try", "var", "void", "while");
    private static final Set<String> JAVASCRIPT = Set.of("async", "await", "break", "case", "catch", "class", "const",
            "continue", "default", "delete", "do", "else", "export", "extends", "false", "finally", "for", "function",
            "if", "import", "in", "instanceof", "let", "new", "null", "of", "return", "super", "switch", "this", "throw",
            "true", "try", "typeof", "undefined", "var", "void", "while", "yield");
    /** Variables every script gets. */
    private static final Set<String> BUILTINS = Set.of("m0", "client", "player", "world", "options", "server", "source", "event", "mc");

    private ScriptSyntax() {
    }

    public static String normalizeLanguage(String language) {
        String lower = language == null ? "" : language.toLowerCase(Locale.ROOT);
        if (lower.startsWith("groovy")) {
            return "groovy";
        }
        if (lower.startsWith("javascript") || lower.equals("js")) {
            return "javascript";
        }
        return "kotlin";
    }

    public static boolean isKeyword(String word, String language) {
        return switch (normalizeLanguage(language)) {
            case "groovy" -> GROOVY.contains(word);
            case "javascript" -> JAVASCRIPT.contains(word);
            default -> KOTLIN.contains(word);
        };
    }

    /** The state at the start of every line (index i = state before line i). */
    public static List<State> startStates(List<String> lines, String language) {
        List<State> states = new ArrayList<>(lines.size());
        State state = State.CODE;
        for (String line : lines) {
            states.add(state);
            state = line(line, language, state).endState();
        }
        return states;
    }

    public static LineResult line(String line, String language, State start) {
        String lang = normalizeLanguage(language);
        boolean js = lang.equals("javascript");
        List<Span> spans = new ArrayList<>();
        int n = line.length();
        int i = 0;
        State state = start == null ? State.CODE : start;

        // Finish whatever was left open by the previous line.
        if (state != State.CODE) {
            String closer = switch (state) {
                case BLOCK_COMMENT -> "*/";
                case TRIPLE_STRING -> "\"\"\"";
                default -> "`";
            };
            int end = indexOfUnescaped(line, closer, 0, state != State.BLOCK_COMMENT);
            Kind kind = state == State.BLOCK_COMMENT ? Kind.COMMENT : Kind.STRING;
            if (end < 0) {
                spans.add(new Span(0, n, kind));
                return new LineResult(spans, state);
            }
            i = end + closer.length();
            spans.add(new Span(0, i, kind));
            state = State.CODE;
        }

        while (i < n) {
            char c = line.charAt(i);
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                spans.add(new Span(i, n, Kind.COMMENT));
                return new LineResult(spans, State.CODE);
            }
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '*') {
                int end = line.indexOf("*/", i + 2);
                if (end < 0) {
                    spans.add(new Span(i, n, Kind.COMMENT));
                    return new LineResult(spans, State.BLOCK_COMMENT);
                }
                spans.add(new Span(i, end + 2, Kind.COMMENT));
                i = end + 2;
                continue;
            }
            if (!js && line.startsWith("\"\"\"", i)) {
                int end = line.indexOf("\"\"\"", i + 3);
                if (end < 0) {
                    spans.add(new Span(i, n, Kind.STRING));
                    return new LineResult(spans, State.TRIPLE_STRING);
                }
                spans.add(new Span(i, end + 3, Kind.STRING));
                i = end + 3;
                continue;
            }
            if (js && c == '`') {
                int end = indexOfUnescaped(line, "`", i + 1, true);
                if (end < 0) {
                    spans.add(new Span(i, n, Kind.STRING));
                    return new LineResult(spans, State.TEMPLATE_STRING);
                }
                spans.add(new Span(i, end + 1, Kind.STRING));
                i = end + 1;
                continue;
            }
            if (c == '"' || c == '\'') {
                int end = indexOfUnescaped(line, String.valueOf(c), i + 1, true);
                int stop = end < 0 ? n : end + 1;
                spans.add(new Span(i, stop, Kind.STRING));
                i = stop;
                continue;
            }
            if (c == '@' && i + 1 < n && Character.isJavaIdentifierStart(line.charAt(i + 1))) {
                int j = i + 1;
                while (j < n && Character.isJavaIdentifierPart(line.charAt(j))) {
                    j++;
                }
                spans.add(new Span(i, j, Kind.ANNOTATION));
                i = j;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(line.charAt(i + 1))
                    && (i == 0 || !Character.isJavaIdentifierPart(line.charAt(i - 1))))) {
                int j = i + 1;
                if (c == '0' && j < n && (line.charAt(j) == 'x' || line.charAt(j) == 'X')) {
                    j++;
                    while (j < n && Character.digit(line.charAt(j), 16) >= 0) {
                        j++;
                    }
                } else {
                    while (j < n && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '_'
                            || (line.charAt(j) == '.' && j + 1 < n && Character.isDigit(line.charAt(j + 1))))) {
                        j++;
                    }
                    if (j < n && (line.charAt(j) == 'e' || line.charAt(j) == 'E')) {
                        int k = j + 1;
                        if (k < n && (line.charAt(k) == '+' || line.charAt(k) == '-')) {
                            k++;
                        }
                        if (k < n && Character.isDigit(line.charAt(k))) {
                            j = k;
                            while (j < n && Character.isDigit(line.charAt(j))) {
                                j++;
                            }
                        }
                    }
                }
                while (j < n && "fFdDlLuU".indexOf(line.charAt(j)) >= 0) {
                    j++;
                }
                spans.add(new Span(i, j, Kind.NUMBER));
                i = j;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int j = i + 1;
                while (j < n && Character.isJavaIdentifierPart(line.charAt(j))) {
                    j++;
                }
                String word = line.substring(i, j);
                spans.add(new Span(i, j, classifyWord(line, j, word, lang)));
                i = j;
                continue;
            }
            if ("{}()[];,.:=<>+-*/%!&|?".indexOf(c) >= 0) {
                spans.add(new Span(i, i + 1, Kind.PUNCTUATION));
            } else {
                spans.add(new Span(i, i + 1, Kind.PLAIN));
            }
            i++;
        }
        return new LineResult(spans, State.CODE);
    }

    private static Kind classifyWord(String line, int end, String word, String lang) {
        if (isKeyword(word, lang)) {
            return Kind.KEYWORD;
        }
        if (BUILTINS.contains(word)) {
            return Kind.BUILTIN;
        }
        int k = end;
        while (k < line.length() && line.charAt(k) == ' ') {
            k++;
        }
        if (k < line.length() && (line.charAt(k) == '(' || (line.charAt(k) == '{' && !lang.equals("javascript")))) {
            return Kind.FUNCTION;
        }
        if (Character.isUpperCase(word.charAt(0))) {
            return Kind.TYPE;
        }
        return Kind.PLAIN;
    }

    /** Index of {@code token} from {@code from}, skipping ones preceded by a backslash when {@code escapes}. */
    private static int indexOfUnescaped(String line, String token, int from, boolean escapes) {
        int i = from;
        while (i <= line.length() - token.length()) {
            char c = line.charAt(i);
            if (escapes && c == '\\') {
                i += 2;
                continue;
            }
            if (line.startsWith(token, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /** One Dark-style colours. */
    public static int color(Kind kind) {
        return switch (kind) {
            case KEYWORD -> 0xFFC678DD;
            case TYPE -> 0xFFE5C07B;
            case STRING -> 0xFF98C379;
            case NUMBER -> 0xFFD19A66;
            case COMMENT -> 0xFF7F848E;
            case ANNOTATION -> 0xFF56B6C2;
            case FUNCTION -> 0xFF61AFEF;
            case BUILTIN -> 0xFFE06C75;
            case PUNCTUATION -> 0xFFABB2BF;
            default -> 0xFFDCDFE4;
        };
    }
}
