package me.m0dii.modules.macros.gui;

import me.m0dii.utils.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class MacroWorkbenchTextStyling {
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private MacroWorkbenchTextStyling() {
    }

    public record TextRun(String text, int color) {
    }

    public static List<TextRun> parseColorRuns(String raw, int defaultColor) {
        List<TextRun> runs = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            runs.add(new TextRun("", defaultColor));
            return runs;
        }

        StringBuilder chunk = new StringBuilder();
        int color = defaultColor;
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '&' && i + 1 < raw.length()) {
                int mapped = ColorUtils.mapLegacyColor(raw.charAt(i + 1));
                if (mapped != Integer.MIN_VALUE) {
                    if (!chunk.isEmpty()) {
                        runs.add(new TextRun(chunk.toString(), color));
                        chunk.setLength(0);
                    }
                    color = mapped;
                    i += 2;
                    continue;
                }
            }

            if (c == '#') {
                int end = Math.min(raw.length(), i + 7);
                String token = raw.substring(i, end);
                if (token.length() == 7 && HEX_COLOR.matcher(token).matches()) {
                    if (!chunk.isEmpty()) {
                        runs.add(new TextRun(chunk.toString(), color));
                        chunk.setLength(0);
                    }
                    color = 0xFF000000 | Integer.parseInt(token.substring(1), 16);
                    i += 7;
                    continue;
                }
            }

            chunk.append(c);
            i++;
        }

        if (!chunk.isEmpty() || runs.isEmpty()) {
            runs.add(new TextRun(chunk.toString(), color));
        }
        return runs;
    }
}
