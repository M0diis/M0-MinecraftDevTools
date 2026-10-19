package me.m0dii.modules.scripting.ide;

import me.m0dii.modules.scripting.ScriptDirectives;

import java.util.*;

/**
 * Small text helpers shared by the script editor: line splitting, word checks and the {@code //@async} directive.
 */
public final class ScriptText {
    private ScriptText() {
    }

    private static final String ASYNC_DIRECTIVE = "@async";

    public static int countLines(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    public static boolean containsIgnoreCase(Collection<String> names, String target) {
        if (target == null || target.isBlank() || names == null || names.isEmpty()) {
            return false;
        }
        for (String name : names) {
            if (name != null && name.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWord(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == ':';
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("\r\n", "\n").replace('\r', '\n');
    }

    public static boolean scriptRunsAsync(String source) {
        return ScriptDirectives.isAsync(normalize(source));
    }

    public static String withAsyncDirective(String source, boolean enabled) {
        List<String> lines = new ArrayList<>(splitLines(normalize(source)));
        String directiveLine = "// @async " + (enabled ? "on" : "off");
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i) == null ? "" : lines.get(i).trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("//") || t.startsWith("#") || t.startsWith("/*") || t.startsWith("*")) {
                if (t.contains(ASYNC_DIRECTIVE)) {
                    lines.set(i, directiveLine);
                    return String.join("\n", lines);
                }
                continue;
            }
            lines.add(i, directiveLine);
            return String.join("\n", lines);
        }
        lines.addFirst(directiveLine);
        return String.join("\n", lines);
    }

    public static List<String> splitLines(String source) {
        String[] parts = normalize(source).split("\\n", -1);
        List<String> out = new ArrayList<>(parts.length);
        out.addAll(Arrays.asList(parts));
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }
}
