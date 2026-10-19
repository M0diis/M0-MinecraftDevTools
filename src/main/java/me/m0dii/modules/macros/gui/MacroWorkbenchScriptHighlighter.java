package me.m0dii.modules.macros.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tokenizer behind the syntax colouring of button scripts in the workbench editor.
 */
public final class MacroWorkbenchScriptHighlighter {
    private MacroWorkbenchScriptHighlighter() {
    }

    private static final Set<String> SCRIPT_HIGHLIGHT_KEYWORDS = Set.of(
            "def", "class", "if", "else", "for", "while", "return", "import", "new",
            "true", "false", "null", "fun", "val", "var", "object", "when",
            "try", "catch", "finally", "throw", "in", "as", "is"
    );

    public static List<ScriptTokenSpan> tokenizeButtonScriptLine(String line) {
        List<ScriptTokenSpan> out = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return out;
        }
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (i + 1 < line.length() && c == '/' && line.charAt(i + 1) == '/') {
                out.add(new ScriptTokenSpan(i, line.length(), 0xFF6FA86F));
                break;
            }
            if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < line.length()) {
                    if (line.charAt(j) == c && line.charAt(j - 1) != '\\') {
                        j++;
                        break;
                    }
                    j++;
                }
                out.add(new ScriptTokenSpan(i, Math.min(j, line.length()), 0xFFE6C07B));
                i = j;
                continue;
            }
            if (c == '{') {
                int close = line.indexOf('}', i + 1);
                if (close > i + 1 && isScriptPlaceholderToken(line.substring(i + 1, close))) {
                    out.add(new ScriptTokenSpan(i, close + 1, 0xFF56B6C2));
                    i = close + 1;
                    continue;
                }
            }
            if (Character.isDigit(c)) {
                int j = i + 1;
                while (j < line.length() && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '.')) {
                    j++;
                }
                out.add(new ScriptTokenSpan(i, j, 0xFFD19A66));
                i = j;
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int j = i + 1;
                while (j < line.length() && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '_')) {
                    j++;
                }
                String token = line.substring(i, j);
                int color = SCRIPT_HIGHLIGHT_KEYWORDS.contains(token) ? 0xFF61AFEF : 0xFFDCDCAA;
                out.add(new ScriptTokenSpan(i, j, color));
                i = j;
                continue;
            }
            out.add(new ScriptTokenSpan(i, i + 1, 0xFFEAEAEA));
            i++;
        }
        return out;
    }

    private static boolean isScriptPlaceholderToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char ch = token.charAt(i);
            if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '-')) {
                return false;
            }
        }
        return true;
    }

    public record ScriptTokenSpan(int start, int end, int color) {
    }
}
