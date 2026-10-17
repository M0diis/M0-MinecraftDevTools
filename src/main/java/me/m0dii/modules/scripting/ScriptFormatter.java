package me.m0dii.modules.scripting;

/**
 * Re-indents Groovy/Kotlin/JavaScript code from its brackets: each line gets one indent per open
 * {@code { ( [} before it, lines starting with a closing bracket are dedented. Text inside strings, comments,
 * block comments and multi-line strings is never changed.
 */
public final class ScriptFormatter {
    private ScriptFormatter() {
    }

    public static String reindent(String source, String indentUnit) {
        if (source == null || source.isEmpty()) {
            return source == null ? "" : source;
        }
        String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder(source.length() + 64);
        State state = new State();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i > 0) {
                out.append('\n');
            }
            if (state.inBlockComment || state.inTripleString || state.inTemplate) {
                // Inside a multi-line construct: keep the line exactly as written.
                out.append(line);
                scan(line, state);
                continue;
            }
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            int leadingClosers = 0;
            while (leadingClosers < trimmed.length() && "}])".indexOf(trimmed.charAt(leadingClosers)) >= 0) {
                leadingClosers++;
            }
            int level = Math.max(0, state.depth - leadingClosers);
            out.append(indentUnit.repeat(level)).append(trimmed);
            scan(trimmed, state);
        }
        return out.toString();
    }

    private static final class State {
        int depth;
        boolean inBlockComment;
        boolean inTripleString;
        boolean inTemplate;
    }

    private static void scan(String line, State state) {
        int i = 0;
        int n = line.length();
        while (i < n) {
            char c = line.charAt(i);
            if (state.inBlockComment) {
                if (c == '*' && i + 1 < n && line.charAt(i + 1) == '/') {
                    state.inBlockComment = false;
                    i += 2;
                } else {
                    i++;
                }
                continue;
            }
            if (state.inTripleString) {
                if (line.startsWith("\"\"\"", i) || line.startsWith("'''", i)) {
                    state.inTripleString = false;
                    i += 3;
                } else {
                    i++;
                }
                continue;
            }
            if (state.inTemplate) {
                if (c == '\\') {
                    i += 2;
                } else if (c == '`') {
                    state.inTemplate = false;
                    i++;
                } else {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                return; // line comment
            }
            if (c == '#' && i == 0) {
                return; // shebang / hash comment at line start
            }
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '*') {
                state.inBlockComment = true;
                i += 2;
                continue;
            }
            if (line.startsWith("\"\"\"", i) || line.startsWith("'''", i)) {
                state.inTripleString = true;
                i += 3;
                continue;
            }
            if (c == '`') {
                state.inTemplate = true;
                i++;
                continue;
            }
            if (c == '"' || c == '\'') {
                i = skipString(line, i, c);
                continue;
            }
            if (c == '{' || c == '(' || c == '[') {
                state.depth++;
            } else if (c == '}' || c == ')' || c == ']') {
                state.depth = Math.max(0, state.depth - 1);
            }
            i++;
        }
    }

    /** Returns the index after the closing quote (or the end of the line). */
    private static int skipString(String line, int start, char quote) {
        int i = start + 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i + 1;
            }
            i++;
        }
        return line.length();
    }
}
