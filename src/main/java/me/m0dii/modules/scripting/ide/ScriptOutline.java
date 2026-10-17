package me.m0dii.modules.scripting.ide;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Functions, classes and top-level values in a script, for the outline / go-to-symbol list. */
public final class ScriptOutline {
    public record Symbol(String name, String kind, int line, int column) {
        public String label() {
            return kind + " " + name;
        }
    }

    private static final Pattern KOTLIN = Pattern.compile(
            "^\\s*(?:(?:private|public|internal|protected|inline|suspend|override|open|data|enum|sealed|abstract)\\s+)*"
                    + "(fun|class|object|interface|val|var|const val)\\s+(?:<[^>]*>\\s*)?(?:[A-Za-z_][\\w.]*\\.)?([A-Za-z_]\\w*)");
    private static final Pattern GROOVY = Pattern.compile(
            "^\\s*(?:(?:private|public|protected|static|final|abstract)\\s+)*"
                    + "(class|interface|enum|def|void|[A-Z]\\w*(?:<[^>]*>)?|int|long|double|float|boolean)\\s+([A-Za-z_]\\w*)\\s*(\\(|=|\\{|$)");
    private static final Pattern JAVASCRIPT = Pattern.compile(
            "^\\s*(?:export\\s+)?(?:async\\s+)?(function\\*?|class|const|let|var)\\s+([A-Za-z_$][\\w$]*)");

    private ScriptOutline() {
    }

    public static List<Symbol> symbols(String text, String language) {
        String lang = ScriptSyntax.normalizeLanguage(language);
        Pattern pattern = switch (lang) {
            case "groovy" -> GROOVY;
            case "javascript" -> JAVASCRIPT;
            default -> KOTLIN;
        };
        List<Symbol> out = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        List<ScriptSyntax.State> states = ScriptSyntax.startStates(List.of(lines), lang);
        for (int i = 0; i < lines.length; i++) {
            if (states.get(i) != ScriptSyntax.State.CODE) {
                continue;
            }
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                continue;
            }
            Matcher m = pattern.matcher(line);
            if (!m.find()) {
                continue;
            }
            String kind = m.group(1);
            if (lang.equals("groovy") && !List.of("class", "interface", "enum", "def").contains(kind)) {
                // Typed Groovy declarations: only methods (followed by "(") and fields at the top level.
                if (!"(".equals(m.group(3)) && !line.startsWith(kind)) {
                    continue;
                }
                kind = "(".equals(m.group(3)) ? "method" : "field";
            } else if (lang.equals("groovy") && kind.equals("def")) {
                kind = "(".equals(m.group(3)) ? "def fun" : "def";
            }
            if (lang.equals("kotlin") && (kind.equals("val") || kind.equals("var")) && indentOf(line) > 0) {
                continue; // locals inside functions are noise
            }
            if (lang.equals("javascript") && (kind.equals("let") || kind.equals("var") || kind.equals("const"))
                    && indentOf(line) > 0) {
                continue;
            }
            out.add(new Symbol(m.group(2), kind, i, m.start(2)));
        }
        return out;
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return i;
    }
}
