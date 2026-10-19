package me.m0dii.modules.scripting.ide;

import me.m0dii.modules.scripting.ScriptTypes;

import java.util.Collection;
import java.util.Locale;

import static me.m0dii.modules.scripting.ide.ScriptText.containsIgnoreCase;

/**
 * Script file naming: supported languages, extensions, new-script templates and name normalisation.
 */
public final class ScriptNames {
    private ScriptNames() {
    }

    public static final String DEFAULT_SCRIPT_NAME = "untitled.kts";

    public static final String GROOVY_EXT = ScriptTypes.GROOVY_EXT;

    public static final String KOTLIN_EXT = ScriptTypes.KOTLIN_EXT;

    public static final String JAVASCRIPT_EXT = ScriptTypes.JAVASCRIPT_EXT;

    public enum Language {GROOVY, KOTLIN, JAVASCRIPT}

    /** Starter text for a new script in its language. */
    public static String newScriptTemplate(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(GROOVY_EXT)) {
            return "// @async off\n// Groovy script. m0 is the helper: m0.message(\"hi\"), m0.command(\"time set day\")\n\nm0.message(\"Hello from \" + player.name.string)\n";
        }
        if (lower.endsWith(JAVASCRIPT_EXT)) {
            return "// @async off\n// JavaScript. m0 is the helper: m0.message('hi'), m0.command('time set day')\n\nm0.message('Hello from ' + player.getName().getString());\n";
        }
        return "// @async off\n// Kotlin script. m0 is the helper: m0.message(\"hi\"), m0.command(\"time set day\")\n\nm0.message(\"Hello from ${player.name.string}\")\n";
    }

    public static String nextName(String existing, String suffix, Collection<String> takenNames) {
        int dot = existing.lastIndexOf('.');
        String base = dot < 0 ? existing : existing.substring(0, dot);
        String ext = dot < 0 ? "" : existing.substring(dot);
        String candidate = base + suffix + ext;
        int idx = 2;
        while (containsIgnoreCase(takenNames, candidate)) {
            candidate = base + suffix + "_" + idx + ext;
            idx++;
        }
        return candidate;
    }

    public static String normalizeScriptNameForRename(String raw, String current) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return normalizeScriptName(current);
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(GROOVY_EXT) || lower.endsWith(KOTLIN_EXT) || lower.endsWith(JAVASCRIPT_EXT)) {
            return name;
        }
        String currentLower = current == null ? "" : current.toLowerCase(Locale.ROOT);
        if (currentLower.endsWith(GROOVY_EXT)) {
            return name + GROOVY_EXT;
        }
        if (currentLower.endsWith(JAVASCRIPT_EXT)) {
            return name + JAVASCRIPT_EXT;
        }
        return name + KOTLIN_EXT;
    }

    public static String normalizeScriptName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return DEFAULT_SCRIPT_NAME;
        }
        if (name.endsWith(GROOVY_EXT) || name.endsWith(KOTLIN_EXT) || name.endsWith(JAVASCRIPT_EXT)) {
            return name;
        }
        return name + KOTLIN_EXT;
    }

    public static Language inferLanguage(String file) {
        String lower = file == null ? "" : file.toLowerCase(Locale.ROOT);
        if (lower.endsWith(GROOVY_EXT)) {
            return Language.GROOVY;
        }
        if (lower.endsWith(JAVASCRIPT_EXT)) {
            return Language.JAVASCRIPT;
        }
        return Language.KOTLIN;
    }
}
