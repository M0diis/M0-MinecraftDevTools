package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.hud.MacroHudDataHandler;

public final class MacroWorkbenchUiOps {
    private MacroWorkbenchUiOps() {
    }

    public static MacroHudDataHandler.HorizontalAlign cycleHorizontalAlign(MacroHudDataHandler.HorizontalAlign current, boolean forward) {
        return switch (current) {
            case LEFT -> forward ? MacroHudDataHandler.HorizontalAlign.CENTER : MacroHudDataHandler.HorizontalAlign.RIGHT;
            case CENTER -> forward ? MacroHudDataHandler.HorizontalAlign.RIGHT : MacroHudDataHandler.HorizontalAlign.LEFT;
            case RIGHT -> forward ? MacroHudDataHandler.HorizontalAlign.LEFT : MacroHudDataHandler.HorizontalAlign.CENTER;
        };
    }

    public static MacroHudDataHandler.VerticalAlign cycleVerticalAlign(MacroHudDataHandler.VerticalAlign current, boolean forward) {
        return switch (current) {
            case TOP -> forward ? MacroHudDataHandler.VerticalAlign.CENTER : MacroHudDataHandler.VerticalAlign.BOTTOM;
            case CENTER -> forward ? MacroHudDataHandler.VerticalAlign.BOTTOM : MacroHudDataHandler.VerticalAlign.TOP;
            case BOTTOM -> forward ? MacroHudDataHandler.VerticalAlign.TOP : MacroHudDataHandler.VerticalAlign.CENTER;
        };
    }

    public static void cycleBorderSetting(MacroHudDataHandler.HudElement element, boolean forward) {
        if (element == null) {
            return;
        }
        if (!element.drawBorder) {
            element.drawBorder = true;
            element.borderMode = MacroHudDataHandler.BorderMode.FULL;
            return;
        }
        MacroHudDataHandler.BorderMode next = cycleBorderMode(element.borderMode, forward);
        if (next == null) {
            element.drawBorder = false;
            return;
        }
        element.borderMode = next;
    }

    public static String borderModeLabel(MacroHudDataHandler.HudElement element) {
        if (element == null || !element.drawBorder) {
            return "Border: OFF";
        }
        MacroHudDataHandler.BorderMode mode = element.borderMode == null
                ? MacroHudDataHandler.BorderMode.FULL
                : element.borderMode;
        return "Border: " + mode.name();
    }

    public static String backgroundLabel(MacroHudDataHandler.HudElement element) {
        if (element == null || !element.drawBackground) {
            return "BG: OFF";
        }
        int alpha = Math.clamp(element.backgroundAlpha, 0, 255);
        int pct = Math.round((alpha / 255.0f) * 100.0f);
        return element.backgroundOpaque ? "BG:" + pct + "% O" : "BG:" + pct + "%";
    }

    public static void adjustBackgroundAlpha(MacroHudDataHandler.HudElement element, int delta) {
        if (element == null) {
            return;
        }
        int current = Math.clamp(element.backgroundAlpha, 0, 255);
        int next = Math.clamp(current + delta, 0, 255);
        element.backgroundAlpha = next;
        element.backgroundColor = (next << 24) | (element.backgroundColor & 0x00FFFFFF);
    }

    public static String cyclePreset(String current, String[] presets, boolean forward) {
        if (presets == null || presets.length == 0) {
            return current == null ? "" : current;
        }
        String now = current == null ? "" : current;
        int index = 0;
        for (int i = 0; i < presets.length; i++) {
            if (now.equals(presets[i])) {
                index = i;
                break;
            }
        }
        index += forward ? 1 : -1;
        if (index < 0) {
            index = presets.length - 1;
        }
        if (index >= presets.length) {
            index = 0;
        }
        return presets[index];
    }

    public static MacroHudDataHandler.Anchor cycleAnchor(MacroHudDataHandler.Anchor current, boolean forward) {
        MacroHudDataHandler.Anchor[] order = {
                MacroHudDataHandler.Anchor.TOP_LEFT,
                MacroHudDataHandler.Anchor.TOP_CENTER,
                MacroHudDataHandler.Anchor.TOP_RIGHT,
                MacroHudDataHandler.Anchor.MIDDLE_RIGHT,
                MacroHudDataHandler.Anchor.MIDDLE_CENTER,
                MacroHudDataHandler.Anchor.MIDDLE_LEFT,
                MacroHudDataHandler.Anchor.BOTTOM_RIGHT,
                MacroHudDataHandler.Anchor.BOTTOM_CENTER,
                MacroHudDataHandler.Anchor.BOTTOM_LEFT,
        };
        MacroHudDataHandler.Anchor base = current == MacroHudDataHandler.Anchor.CENTER ? MacroHudDataHandler.Anchor.MIDDLE_CENTER : current;
        int idx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == base) {
                idx = i;
                break;
            }
        }
        int next = forward ? idx + 1 : idx - 1;
        if (next < 0) {
            next = order.length - 1;
        }
        if (next >= order.length) {
            next = 0;
        }
        return order[next];
    }

    public static MacroHudDataHandler.VisibilityMode cycleVisibilityMode(MacroHudDataHandler.VisibilityMode mode, boolean forward) {
        MacroHudDataHandler.VisibilityMode[] values = MacroHudDataHandler.VisibilityMode.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mode) {
                index = i;
                break;
            }
        }
        index += forward ? 1 : -1;
        if (index < 0) {
            index = values.length - 1;
        }
        if (index >= values.length) {
            index = 0;
        }
        return values[index];
    }

    public static MacroHudDataHandler.ButtonExecutionMode cycleButtonExecutionMode(MacroHudDataHandler.ButtonExecutionMode mode, boolean forward) {
        MacroHudDataHandler.ButtonExecutionMode[] values = MacroHudDataHandler.ButtonExecutionMode.values();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == mode) {
                index = i;
                break;
            }
        }
        index += forward ? 1 : -1;
        if (index < 0) {
            index = values.length - 1;
        }
        if (index >= values.length) {
            index = 0;
        }
        return values[index];
    }

    public static void ensureVisibleBackground(MacroHudDataHandler.HudElement element) {
        if (element == null || !element.drawBackground) {
            return;
        }
        if ((element.backgroundColor >>> 24) == 0) {
            element.backgroundColor = 0xAA101010;
        }
        if (element.backgroundAlpha <= 0) {
            element.backgroundAlpha = (element.backgroundColor >>> 24) & 0xFF;
            if (element.backgroundAlpha <= 0) {
                element.backgroundAlpha = 0xAA;
            }
        }
        if (element.height < 14) {
            element.height = 14;
        }
        if ((element.borderColor >>> 24) == 0) {
            element.borderColor = 0xFFFFFFFF;
        }
    }

    public static int cycleStyleColor(int current, boolean forward, int[] palette) {
        if (palette == null || palette.length == 0) {
            return current;
        }
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == current) {
                int next = forward ? i + 1 : i - 1;
                if (next < 0) {
                    next = palette.length - 1;
                }
                if (next >= palette.length) {
                    next = 0;
                }
                return palette[next];
            }
        }
        return palette[0];
    }

    private static MacroHudDataHandler.BorderMode cycleBorderMode(MacroHudDataHandler.BorderMode current, boolean forward) {
        MacroHudDataHandler.BorderMode[] order = {
                MacroHudDataHandler.BorderMode.FULL,
                MacroHudDataHandler.BorderMode.LEFT,
                MacroHudDataHandler.BorderMode.RIGHT,
                MacroHudDataHandler.BorderMode.TOP,
                MacroHudDataHandler.BorderMode.BOTTOM
        };
        MacroHudDataHandler.BorderMode base = current == null ? MacroHudDataHandler.BorderMode.FULL : current;
        int idx = 0;
        for (int i = 0; i < order.length; i++) {
            if (order[i] == base) {
                idx = i;
                break;
            }
        }
        int next = forward ? idx + 1 : idx - 1;
        if (next < 0 || next >= order.length) {
            return null;
        }
        return order[next];
    }
}
