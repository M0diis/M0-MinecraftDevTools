package me.m0dii.modules.scripting;

import java.util.Locale;

/**
 * Parses header directives at the top of a script, e.g. {@code // @async on}.
 * Only leading comment lines are considered; the first code line ends the header.
 */
public final class ScriptDirectives {
    public static final String ASYNC_DIRECTIVE = "@async";

    private ScriptDirectives() {
    }

    public static boolean isAsync(String source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        for (String line : source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String t = line.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("//") || t.startsWith("#") || t.startsWith("/*") || t.startsWith("*")) {
                int idx = t.indexOf(ASYNC_DIRECTIVE);
                if (idx >= 0) {
                    String tail = t.substring(idx + ASYNC_DIRECTIVE.length()).trim();
                    if (tail.startsWith(":")) {
                        tail = tail.substring(1).trim();
                    }
                    return !tail.startsWith("off") && !tail.startsWith("false") && !tail.startsWith("0") && !tail.startsWith("no");
                }
                continue;
            }
            break;
        }
        return false;
    }
}
