package me.m0dii.modules.scripting.ide;

import me.m0dii.modules.scripting.ide.ScriptNames.Language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import static me.m0dii.modules.scripting.ide.ScriptText.splitLines;

/**
 * Editor-independent script checks: quick syntax scanning, Kotlin validation source wrapping and compiler message cleanup.
 */
public final class ScriptValidation {
    private ScriptValidation() {
    }

    public record Diagnostic(String severity, String file, int line, int column, String message) {
        public String rowText() {
            String loc = file + ":" + Math.max(1, line) + ":" + Math.max(1, column);
            return "[" + severity + "] " + loc + " " + message;
        }
    }

    public record KotlinValidationSource(String source, int insertedAfterOriginalLine) {
    }

    public static List<Diagnostic> lightweightValidation(String source, Language language) {
        String text = source == null ? "" : source;
        List<Diagnostic> out = new ArrayList<>();
        int line = 1;
        int round = 0;
        int curly = 0;
        int square = 0;
        boolean inString = false;
        char quote = 0;
        int stringLine = 1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
            }
            if (inString) {
                if (quote == '"' && i + 1 < text.length() && c == '$' && text.charAt(i + 1) == '{'
                        && (language == Language.KOTLIN || language == Language.GROOVY)) {
                    InterpolationScan scan = scanInterpolationExpression(text, i + 2, line);
                    line = scan.line();
                    if (scan.unclosed()) {
                        out.add(new Diagnostic("ERROR", "", line, 1, "Unclosed string interpolation expression."));
                        break;
                    }
                    i = scan.endIndex();
                    continue;
                }
                if (c == quote && (i == 0 || text.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                inString = true;
                quote = c;
                stringLine = line;
                continue;
            }
            if (c == '(') {
                round++;
            } else if (c == ')') {
                round--;
            } else if (c == '{') {
                curly++;
            } else if (c == '}') {
                curly--;
            } else if (c == '[') {
                square++;
            } else if (c == ']') {
                square--;
            }

            if (round < 0 || curly < 0 || square < 0) {
                out.add(new Diagnostic("ERROR", "", line, 1, "Unexpected closing bracket."));
                round = Math.max(0, round);
                curly = Math.max(0, curly);
                square = Math.max(0, square);
            }
        }
        if (inString) {
            out.add(new Diagnostic("ERROR", "", stringLine, 1, "Unclosed string literal."));
        }
        if (round > 0) {
            out.add(new Diagnostic("ERROR", "", line, 1, "Unclosed '()' group."));
        }
        if (curly > 0) {
            out.add(new Diagnostic("ERROR", "", line, 1, "Unclosed '{}' block."));
        }
        if (square > 0) {
            out.add(new Diagnostic("ERROR", "", line, 1, "Unclosed '[]' group."));
        }
        return out;
    }

    public record InterpolationScan(int endIndex, int line, boolean unclosed) {
    }

    private static InterpolationScan scanInterpolationExpression(String text, int startIndex, int line) {
        int depth = 1;
        int i = startIndex;
        while (i < text.length() && depth > 0) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
            }
            if (c == '"' || c == '\'') {
                char q = c;
                i++;
                while (i < text.length()) {
                    char qc = text.charAt(i);
                    if (qc == '\n') {
                        line++;
                    }
                    if (qc == q && text.charAt(i - 1) != '\\') {
                        break;
                    }
                    i++;
                }
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        if (depth > 0) {
            return new InterpolationScan(Math.max(startIndex, text.length() - 1), line, true);
        }
        return new InterpolationScan(Math.max(startIndex, i - 1), line, false);
    }

    public static List<Diagnostic> dedupeDiagnostics(List<Diagnostic> input) {
        LinkedHashMap<String, Diagnostic> uniq = new LinkedHashMap<>();
        for (Diagnostic d : input) {
            String key = d.severity + "|" + d.file + "|" + d.line + "|" + d.column + "|" + d.message;
            uniq.put(key, d);
        }
        return new ArrayList<>(uniq.values());
    }

    public static int parseIntAfter(String key, String message, int fallback) {
        if (message == null) {
            return fallback;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(key.toLowerCase(Locale.ROOT));
        if (idx < 0) {
            return fallback;
        }
        int i = idx + key.length();
        while (i < lower.length() && !Character.isDigit(lower.charAt(i))) i++;
        int j = i;
        while (j < lower.length() && Character.isDigit(lower.charAt(j))) j++;
        try {
            return Integer.parseInt(lower.substring(i, j));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    public static String cleanupCompilerMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String cleaned = message
                .replaceAll("\\(ScriptingHost[^)]*\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = cleaned.replace("ERROR ERROR", "ERROR");
        return cleaned;
    }

    private static final String KOTLIN_VALIDATION_PREAMBLE = """
            import net.minecraft.client.MinecraftClient
            import net.minecraft.client.network.ClientPlayerEntity
            import net.minecraft.client.option.GameOptions
            import net.minecraft.client.world.ClientWorld
            import net.minecraft.server.MinecraftServer

            val client: MinecraftClient? = null
            val source: ClientPlayerEntity? = null
            val player: ClientPlayerEntity? = null
            val world: ClientWorld? = null
            val options: GameOptions? = null
            val server: MinecraftServer? = null

            """;

    private static final int KOTLIN_VALIDATION_PREAMBLE_LINES = splitLines(KOTLIN_VALIDATION_PREAMBLE).size();

    public static KotlinValidationSource wrapKotlinValidationSource(String source) {
        String text = source == null ? "" : source;
        List<String> lines = splitLines(text);
        int insertAtIndex = 0;

        while (insertAtIndex < lines.size()) {
            String trimmed = lines.get(insertAtIndex).trim();
            if (trimmed.startsWith("import ")) {
                insertAtIndex++;
                continue;
            }
            if (trimmed.isEmpty() && insertAtIndex > 0) {
                insertAtIndex++;
                continue;
            }
            break;
        }

        StringBuilder builder = new StringBuilder(text.length() + KOTLIN_VALIDATION_PREAMBLE.length() + 32);
        for (int i = 0; i < lines.size(); i++) {
            if (i == insertAtIndex) {
                builder.append(KOTLIN_VALIDATION_PREAMBLE);
            }
            builder.append(lines.get(i));
            if (i < lines.size() - 1) {
                builder.append('\n');
            }
        }
        if (lines.isEmpty() || insertAtIndex >= lines.size()) {
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            builder.append(KOTLIN_VALIDATION_PREAMBLE);
        }

        return new KotlinValidationSource(builder.toString(), insertAtIndex);
    }

    public static int unwrapKotlinValidationLine(String source, int line) {
        KotlinValidationSource wrapped = wrapKotlinValidationSource(source);
        if (line > wrapped.insertedAfterOriginalLine()) {
            return Math.max(1, line - KOTLIN_VALIDATION_PREAMBLE_LINES);
        }
        return Math.max(1, line);
    }
}
