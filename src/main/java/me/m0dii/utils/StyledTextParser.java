package me.m0dii.utils;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;

public final class StyledTextParser {
    private StyledTextParser() {
    }

    public record StyledFragment(String text,
                                 Integer colorArgb,
                                 boolean bold,
                                 boolean italic,
                                 boolean underlined,
                                 boolean strikethrough,
                                 boolean obfuscated) {
    }

    public record ColorRun(String text, int color) {
    }

    public static List<StyledFragment> parseFragments(String raw) {
        String normalized = StringUtils.normalizeMultilineInput(raw);
        if (normalized.isEmpty()) {
            return List.of(new StyledFragment("", null, false, false, false, false, false));
        }

        List<StyledFragment> fragments = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        StyleState style = StyleState.empty();

        int i = 0;
        while (i < normalized.length()) {
            if (isLegacyControlStart(normalized, i)) {
                StyleState next = applyLegacyCode(style, normalized.charAt(i + 1));
                if (next != null) {
                    flushChunk(fragments, chunk, style);
                    style = next;
                    i += 2;
                    continue;
                }
            }

            Integer angleHex = parseAngleHex(normalized, i);
            if (angleHex != null) {
                flushChunk(fragments, chunk, style);
                style = style.withColor(angleHex);
                i += 9;
                continue;
            }

            Integer bareHex = parseBareHex(normalized, i);
            if (bareHex != null) {
                flushChunk(fragments, chunk, style);
                style = style.withColor(bareHex);
                i += 7;
                continue;
            }

            if (normalized.regionMatches(true, i, "<reset>", 0, "<reset>".length())) {
                flushChunk(fragments, chunk, style);
                style = StyleState.empty();
                i += "<reset>".length();
                continue;
            }

            chunk.append(normalized.charAt(i));
            i++;
        }

        flushChunk(fragments, chunk, style);
        if (fragments.isEmpty()) {
            return List.of(new StyledFragment("", null, false, false, false, false, false));
        }
        return List.copyOf(fragments);
    }

    public static List<ColorRun> parseColorRuns(String raw, int defaultColor) {
        List<StyledFragment> fragments = parseFragments(raw);
        List<ColorRun> runs = new ArrayList<>(fragments.size());
        for (StyledFragment fragment : fragments) {
            runs.add(new ColorRun(fragment.text(), fragment.colorArgb() == null ? defaultColor : fragment.colorArgb()));
        }
        return List.copyOf(runs);
    }

    public static Text parseText(String raw) {
        MutableText out = Text.empty();
        for (StyledFragment fragment : parseFragments(raw)) {
            MutableText piece = Text.literal(fragment.text());
            piece.setStyle(toStyle(fragment));
            out.append(piece);
        }
        return out;
    }

    public static List<Text> parseLines(String raw) {
        List<String> lines = StringUtils.splitLinesRaw(raw);
        if (lines.size() == 1 && lines.getFirst().isBlank()) {
            return List.of();
        }
        List<Text> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(parseText(line));
        }
        return List.copyOf(out);
    }

    private static void flushChunk(List<StyledFragment> fragments, StringBuilder chunk, StyleState style) {
        if (chunk.isEmpty()) {
            return;
        }
        fragments.add(new StyledFragment(
                chunk.toString(),
                style.colorArgb(),
                style.bold(),
                style.italic(),
                style.underlined(),
                style.strikethrough(),
                style.obfuscated()
        ));
        chunk.setLength(0);
    }

    private static Style toStyle(StyledFragment fragment) {
        Style style = Style.EMPTY;
        if (fragment.colorArgb() != null) {
            style = style.withColor(TextColor.fromRgb(fragment.colorArgb() & 0x00FFFFFF));
        }
        if (fragment.bold()) {
            style = style.withBold(true);
        }
        if (fragment.italic()) {
            style = style.withItalic(true);
        }
        if (fragment.underlined()) {
            style = style.withUnderline(true);
        }
        if (fragment.strikethrough()) {
            style = style.withStrikethrough(true);
        }
        if (fragment.obfuscated()) {
            style = style.withObfuscated(true);
        }
        return style;
    }

    private static boolean isLegacyControlStart(String raw, int index) {
        return index + 1 < raw.length() && (raw.charAt(index) == '&' || raw.charAt(index) == '§');
    }

    private static StyleState applyLegacyCode(StyleState current, char code) {
        char normalized = Character.toLowerCase(code);
        int mappedColor = ColorUtils.mapLegacyColor(normalized);
        if (mappedColor != Integer.MIN_VALUE) {
            return StyleState.empty().withColor(mappedColor);
        }
        return switch (normalized) {
            case 'k' -> current.withObfuscated(true);
            case 'l' -> current.withBold(true);
            case 'm' -> current.withStrikethrough(true);
            case 'n' -> current.withUnderlined(true);
            case 'o' -> current.withItalic(true);
            case 'r' -> StyleState.empty();
            default -> null;
        };
    }

    private static Integer parseAngleHex(String raw, int index) {
        if (index + 9 > raw.length() || raw.charAt(index) != '<' || raw.charAt(index + 1) != '#'
                || raw.charAt(index + 8) != '>') {
            return null;
        }
        String token = raw.substring(index + 2, index + 8);
        if (!isHex(token)) {
            return null;
        }
        return 0xFF000000 | Integer.parseInt(token, 16);
    }

    private static Integer parseBareHex(String raw, int index) {
        if (index + 7 > raw.length() || raw.charAt(index) != '#') {
            return null;
        }
        String token = raw.substring(index + 1, index + 7);
        if (!isHex(token)) {
            return null;
        }
        return 0xFF000000 | Integer.parseInt(token, 16);
    }

    private static boolean isHex(String raw) {
        if (raw == null || raw.length() != 6) {
            return false;
        }
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private record StyleState(Integer colorArgb,
                              boolean bold,
                              boolean italic,
                              boolean underlined,
                              boolean strikethrough,
                              boolean obfuscated) {
        private static StyleState empty() {
            return new StyleState(null, false, false, false, false, false);
        }

        private StyleState withColor(int argb) {
            return new StyleState(argb, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated);
        }

        private StyleState withBold(boolean value) {
            return new StyleState(this.colorArgb, value, this.italic, this.underlined, this.strikethrough, this.obfuscated);
        }

        private StyleState withItalic(boolean value) {
            return new StyleState(this.colorArgb, this.bold, value, this.underlined, this.strikethrough, this.obfuscated);
        }

        private StyleState withUnderlined(boolean value) {
            return new StyleState(this.colorArgb, this.bold, this.italic, value, this.strikethrough, this.obfuscated);
        }

        private StyleState withStrikethrough(boolean value) {
            return new StyleState(this.colorArgb, this.bold, this.italic, this.underlined, value, this.obfuscated);
        }

        private StyleState withObfuscated(boolean value) {
            return new StyleState(this.colorArgb, this.bold, this.italic, this.underlined, this.strikethrough, value);
        }
    }
}
