package me.m0dii.modules.scripting.ide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Facts about a script file for lists and previews: language, size, async flag and a one-line description. */
public record ScriptInfo(String language, int lines, boolean async, String description) {

    public static ScriptInfo of(String name, String text) {
        String source = text == null ? "" : text.replace("\r\n", "\n");
        String[] lines = source.split("\n", -1);
        int count = source.isEmpty() ? 0 : (source.endsWith("\n") ? lines.length - 1 : lines.length);
        return new ScriptInfo(languageOf(name), count, me.m0dii.modules.scripting.ScriptDirectives.isAsync(source), describe(lines));
    }

    public static String languageOf(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".groovy")) {
            return "Groovy";
        }
        if (lower.endsWith(".js")) {
            return "JavaScript";
        }
        return "Kotlin";
    }

    /** First leading comment line that isn't a directive, e.g. "// Farms bonemeal every 5 seconds". */
    private static String describe(String[] lines) {
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!(line.startsWith("//") || line.startsWith("#") || line.startsWith("/*") || line.startsWith("*"))) {
                return "";
            }
            String body = line.replaceFirst("^(//+|#+|/\\*+|\\*+)", "").replaceFirst("\\*/$", "").trim();
            if (body.isEmpty() || body.startsWith("@")) {
                continue;
            }
            return body;
        }
        return "";
    }

    /** The first {@code max} lines for a preview. */
    public static List<String> preview(String text, int max) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length && i < max; i++) {
            out.add(lines[i].replace("\t", "    "));
        }
        return out;
    }
}
