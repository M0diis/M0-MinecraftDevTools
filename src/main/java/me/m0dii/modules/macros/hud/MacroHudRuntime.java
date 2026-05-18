package me.m0dii.modules.macros.hud;

import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class MacroHudRuntime {

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private MacroHudRuntime() {
    }

    public static void render(@NotNull DrawContext context, boolean interactiveMode) {
        MacroHudDataHandler.HudConfig cfg = MacroHudDataHandler.getConfigCopy();
        if (!cfg.enabled) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        for (MacroHudDataHandler.HudElement element : cfg.elements) {
            if (!element.visible) {
                continue;
            }

            if (element.visibilityMode == MacroHudDataHandler.VisibilityMode.CHAT_ONLY && !isInteractiveContext()) {
                continue;
            }

            int x = resolveX(element, screenW);
            int y = resolveY(element, screenH);

            switch (element.type) {
                case TEXT -> renderTextElement(context, element, x, y);
                case BUTTON -> renderButtonElement(context, element, x, y, interactiveMode);
                case MACRO_KEYBINDS -> renderMacroKeybindElement(context, element, x, y);
            }
        }
    }

    public static boolean handleClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !(client.currentScreen instanceof ChatScreen)) {
            return false;
        }

        MacroHudDataHandler.HudConfig cfg = MacroHudDataHandler.getConfigCopy();
        if (!cfg.enabled) {
            return false;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        List<MacroHudDataHandler.HudElement> reverse = new ArrayList<>(cfg.elements);
        for (int i = reverse.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement element = reverse.get(i);
            if (!element.visible || element.type != MacroHudDataHandler.ElementType.BUTTON) {
                continue;
            }
            if (element.visibilityMode == MacroHudDataHandler.VisibilityMode.CHAT_ONLY && !isInteractiveContext()) {
                continue;
            }
            int x = resolveX(element, screenW);
            int y = resolveY(element, screenH);
            if (!contains(x, y, element, mouseX, mouseY)) {
                continue;
            }

            String actionName = (element.label == null || element.label.isBlank()) ? "HUD Action" : element.label;
            if (element.buttonAction != null && !element.buttonAction.isBlank()) {
                CommandMacros.runInlineAction(actionName, element.buttonAction);
                return true;
            }

            if (element.macroId == null || element.macroId.isBlank()) {
                client.player.sendMessage(Text.literal("HUD button '" + actionName + "' has no macro id or action."), true);
                return true;
            }

            boolean ok = CommandMacros.runMacroById(element.macroId);
            if (!ok) {
                client.player.sendMessage(Text.literal("Unknown macro id: " + element.macroId).formatted(Formatting.RED), true);
            }
            return true;
        }

        return false;
    }

    public static boolean isInteractiveContext() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen instanceof ChatScreen;
    }

    private static void renderTextElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        List<String> lines = splitLines(expanded(element.text));
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        int startY;
        float scale = Math.clamp(element.fontScale, 0.5f, 4.0f);
        int lineHeight = Math.max(6, Math.round(Math.max(6, element.lineHeight) * scale));
        int totalTextHeight = Math.max(8, lines.size() * lineHeight);
        startY = alignedStartY(y, element, totalTextHeight, element.drawBackground);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int yy = startY + i * lineHeight;
            int xx = alignedStartX(x, element, line, element.drawBackground, scale);
            drawStyledTextLine(context, line, xx, yy, element.textColor, scale);
        }
    }

    private static void renderButtonElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y, boolean interactiveMode) {
        int x1 = x;
        int y1 = y;
        int x2 = x + element.width;
        int y2 = y + element.height;
        List<String> lines = splitLines(expanded(element.label));

        if (element.drawBackground) {
            int bg = interactiveMode ? brighten(element.backgroundColor, 0x10101010) : element.backgroundColor;
            context.fill(x1, y1, x2, y2, bg);
        }

        if (element.drawBorder) {
            drawBorder(context, x1, y1, element.width, element.height, element.borderColor);
        }

        float scale = Math.clamp(element.fontScale, 0.5f, 4.0f);
        int lineHeight = Math.max(6, Math.round(Math.max(6, element.lineHeight) * scale));
        int totalTextHeight = Math.max(8, lines.size() * lineHeight);
        int baseY = alignedStartY(y, element, totalTextHeight, true);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textX = alignedStartX(x, element, line, true, scale);
            int textY = baseY + i * lineHeight;
            drawStyledTextLine(context, line, textX, textY, element.textColor, scale);
        }
    }

    private static void renderMacroKeybindElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        List<String> lines = new ArrayList<>();
        String title = expanded(element.text);
        lines.add(title == null || title.isBlank() ? "Macro Keybinds" : title);

        Map<String, MacroDataHandler.MacroEntry> all = MacroDataHandler.getAllMacros();
        if (all.isEmpty()) {
            lines.add("(none)");
        } else {
            for (MacroDataHandler.MacroEntry macro : all.values()) {
                if (macro == null || macro.keyCode < 0) {
                    continue;
                }
                String name = macro.name == null || macro.name.isBlank() ? "Unnamed" : macro.name;
                String key = safeKeyName(macro.keyCode);
                lines.add(name + " - [" + key + "]");
            }
            if (lines.size() == 1) {
                lines.add("(none)");
            }
        }

        renderLinesElement(context, element, x, y, lines);
    }

    private static void renderLinesElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y, List<String> lines) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        float scale = Math.clamp(element.fontScale, 0.5f, 4.0f);
        int lineHeight = Math.max(6, Math.round(Math.max(6, element.lineHeight) * scale));
        int totalTextHeight = Math.max(8, lines.size() * lineHeight);
        int startY = alignedStartY(y, element, totalTextHeight, element.drawBackground);
        int maxBottom = y + element.height - Math.max(2, lineHeight);

        for (int i = 0; i < lines.size(); i++) {
            int yy = startY + i * lineHeight;
            if (yy > maxBottom) {
                break;
            }
            String line = lines.get(i);
            int xx = alignedStartX(x, element, line, element.drawBackground, scale);
            drawStyledTextLine(context, line, xx, yy, element.textColor, scale);
        }
    }

    private static boolean contains(int x, int y, MacroHudDataHandler.HudElement e, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + e.width && mouseY >= y && mouseY <= y + e.height;
    }

    private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        int x2 = x + width;
        int y2 = y + height;
        context.fill(x, y, x2, y + 1, color);
        context.fill(x, y2 - 1, x2, y2, color);
        context.fill(x, y, x + 1, y2, color);
        context.fill(x2 - 1, y, x2, y2, color);
    }

    private static int brighten(int color, int add) {
        long c = (color & 0xFFFFFFFFL);
        long a = c & 0xFF000000L;
        long rgb = c & 0x00FFFFFFL;
        long out = Math.min(0x00FFFFFFL, rgb + (add & 0x00FFFFFFL));
        return (int) (a | out);
    }

    private static String expanded(String raw) {
        MinecraftClient client = MinecraftClient.getInstance();
        try {
            String out = MacroPlaceholders.expand(client, raw);
            return out == null ? "" : out;
        } catch (Exception ignored) {
            return raw == null ? "" : raw;
        }
    }

    private static String safeKeyName(int keyCode) {
        try {
            return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
        } catch (Exception ignored) {
            return "Key " + keyCode;
        }
    }

    private static List<String> splitLines(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of("");
        }
        String normalized = raw.replace("\\n", "\n");
        String[] lines = normalized.split("\\n", -1);
        List<String> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(line == null ? "" : line);
        }
        return out;
    }

    private static int alignedStartX(int x, MacroHudDataHandler.HudElement element, String line, boolean insideBox, float scale) {
        int pad = insideBox ? 4 : 0;
        int textWidth = Math.max(1, Math.round(styledLineWidth(line) * scale));
        return switch (element.horizontalAlign) {
            case LEFT -> x + pad;
            case RIGHT -> x + Math.max(pad, element.width - textWidth - pad);
            case CENTER -> x + (element.width - textWidth) / 2;
        };
    }

    private static int styledLineWidth(String rawLine) {
        int width = 0;
        for (TextRun run : parseColorRuns(rawLine, 0xFFFFFFFF)) {
            width += MinecraftClient.getInstance().textRenderer.getWidth(run.text());
        }
        return width;
    }

    private static void drawStyledTextLine(DrawContext context, String rawLine, int x, int y, int defaultColor, float scale) {
        List<TextRun> runs = parseColorRuns(rawLine, defaultColor);
        if (runs.isEmpty()) {
            return;
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        int logicalX = Math.round(x / scale);
        int logicalY = Math.round(y / scale);
        int cursor = logicalX;

        var tr = MinecraftClient.getInstance().textRenderer;
        for (TextRun run : runs) {
            if (run.text().isEmpty()) {
                continue;
            }
            context.drawTextWithShadow(tr, run.text(), cursor, logicalY, run.color());
            cursor += tr.getWidth(run.text());
        }

        context.getMatrices().popMatrix();
    }

    private record TextRun(String text, int color) {
    }

    private static List<TextRun> parseColorRuns(String raw, int defaultColor) {
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
                int mapped = mapLegacyColor(raw.charAt(i + 1));
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

    private static int mapLegacyColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0xFF000000;
            case '1' -> 0xFF0000AA;
            case '2' -> 0xFF00AA00;
            case '3' -> 0xFF00AAAA;
            case '4' -> 0xFFAA0000;
            case '5' -> 0xFFAA00AA;
            case '6' -> 0xFFFFAA00;
            case '7' -> 0xFFAAAAAA;
            case '8' -> 0xFF555555;
            case '9' -> 0xFF5555FF;
            case 'a' -> 0xFF55FF55;
            case 'b' -> 0xFF55FFFF;
            case 'c' -> 0xFFFF5555;
            case 'd' -> 0xFFFF55FF;
            case 'e' -> 0xFFFFFF55;
            case 'f' -> 0xFFFFFFFF;
            default -> Integer.MIN_VALUE;
        };
    }

    private static int alignedStartY(int y, MacroHudDataHandler.HudElement element, int totalTextHeight, boolean insideBox) {
        int pad = insideBox ? 2 : 0;
        return switch (element.verticalAlign) {
            case TOP -> y + pad;
            case BOTTOM -> y + Math.max(pad, element.height - totalTextHeight - pad);
            case CENTER -> y + Math.max(0, (element.height - totalTextHeight) / 2);
        };
    }

    private static int resolveX(MacroHudDataHandler.HudElement element, int screenW) {
        return switch (element.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> element.x;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - element.width - element.x;
            case CENTER -> (screenW - element.width) / 2 + element.x;
        };
    }

    private static int resolveY(MacroHudDataHandler.HudElement element, int screenH) {
        return switch (element.anchor) {
            case TOP_LEFT, TOP_RIGHT -> element.y;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - element.height - element.y;
            case CENTER -> (screenH - element.height) / 2 + element.y;
        };
    }
}

