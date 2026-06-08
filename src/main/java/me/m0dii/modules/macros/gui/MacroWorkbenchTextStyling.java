package me.m0dii.modules.macros.gui;

import me.m0dii.utils.StyledTextParser;

import java.util.List;

public final class MacroWorkbenchTextStyling {
    private MacroWorkbenchTextStyling() {
    }

    public record TextRun(String text, int color) {
    }

    public static List<TextRun> parseColorRuns(String raw, int defaultColor) {
        return StyledTextParser.parseColorRuns(raw, defaultColor).stream()
                .map(run -> new TextRun(run.text(), run.color()))
                .toList();
    }
}

