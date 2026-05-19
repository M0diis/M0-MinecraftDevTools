package me.m0dii.modules.macros.hud;

import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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
                case ICON -> renderIconElement(context, element, x, y);
                case BAR -> renderBarElement(context, element, x, y);
                case VALUE -> renderValueElement(context, element, x, y);
                case LIST -> renderListElement(context, element, x, y);
                case SHAPE -> renderShapeElement(context, element, x, y);
                case STATE_BADGE -> renderStateBadgeElement(context, element, x, y);
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

    private static void renderIconElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        ItemStack stack = resolveIconStack(element);
        int ix = x + Math.max(0, (element.width - 16) / 2);
        int iy = y + Math.max(0, (element.height - 16) / 2);
        context.drawItem(stack, ix, iy);

        if (element.iconShowCount && stack.getCount() > 1) {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Integer.toString(stack.getCount()), x + element.width - 8, y + element.height - 8, 0xFFFFFFFF);
        }

        if (element.iconShowDurability && stack.isDamageable()) {
            int max = stack.getMaxDamage();
            int remain = Math.max(0, max - stack.getDamage());
            float ratio = max <= 0 ? 0.0f : (remain / (float) max);
            int bw = Math.max(1, Math.round((element.width - 2) * ratio));
            context.fill(x + 1, y + element.height - 2, x + element.width - 1, y + element.height - 1, 0x66000000);
            context.fill(x + 1, y + element.height - 2, x + 1 + bw, y + element.height - 1, blendColor(0xFFFF4444, 0xFF44FF44, ratio));
        }

        if (element.iconShowCooldown) {
            float cd = resolveCooldownProgress(stack);
            if (cd > 0.0f) {
                int overlayH = Math.clamp((int) Math.ceil(16.0f * cd), 0, 16);
                context.fill(ix, iy + 16 - overlayH, ix + 16, iy + 16, 0x88000000);
            }
        }

        String label = safe(element.label);
        if (!label.isBlank()) {
            float scale = Math.max(0.5f, element.fontScale);
            int tx = alignedStartX(x, element, label, element.drawBackground, scale);
            int ty = y + Math.max(1, element.height - Math.max(9, Math.round(9 * scale)));
            drawStyledTextLine(context, label, tx, ty, element.textColor, scale);
        }
    }

    private static void renderBarElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        double min = element.minValue;
        double max = element.maxValue;
        if (!element.sourceTokenMax.isBlank()) {
            Double maxToken = resolveNumericToken(element.sourceTokenMax);
            if (maxToken != null) {
                max = maxToken;
            }
        }
        Double valueToken = resolveNumericToken(element.sourceToken);
        double value = valueToken == null ? min : valueToken;
        if (max <= min) {
            max = min + 1.0;
        }
        float progress = (float) Math.clamp((value - min) / (max - min), 0.0, 1.0);

        int innerX = x + 1;
        int innerY = y + 1;
        int innerW = Math.max(1, element.width - 2);
        int innerH = Math.max(1, element.height - 2);

        if (element.segmented) {
            int segments = Math.max(1, element.segments);
            int gap = 1;
            int segW = Math.max(1, (innerW - (segments - 1) * gap) / segments);
            float filled = progress * segments;
            for (int i = 0; i < segments; i++) {
                int sx = innerX + i * (segW + gap);
                float local = Math.clamp(filled - i, 0.0f, 1.0f);
                int fillW = Math.max(0, Math.round(segW * local));
                context.fill(sx, innerY, sx + segW, innerY + innerH, 0x33000000);
                if (fillW > 0) {
                    float t = segments <= 1 ? progress : (i / (float) (segments - 1));
                    int c = blendColor(element.colorStart, element.colorEnd, t);
                    context.fill(sx, innerY, sx + fillW, innerY + innerH, c);
                }
            }
        } else {
            int fillW = Math.max(0, Math.round(innerW * progress));
            context.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0x33000000);
            for (int px = 0; px < fillW; px++) {
                float t = innerW <= 1 ? progress : (px / (float) (innerW - 1));
                int c = blendColor(element.colorStart, element.colorEnd, t);
                context.fill(innerX + px, innerY, innerX + px + 1, innerY + innerH, c);
            }
        }

        String txt = element.prefix + formatValue(value) + element.suffix;
        int tx = alignedStartX(x, element, txt, true, 1.0f);
        int ty = alignedStartY(y, element, 9, true);
        drawStyledTextLine(context, txt, tx, ty, element.textColor, 1.0f);
    }

    private static void renderValueElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        Double valueToken = resolveNumericToken(element.sourceToken);
        double value = valueToken == null ? 0.0 : valueToken;
        int color = element.textColor;
        if (value <= element.critThreshold) {
            color = element.colorCrit;
        } else if (value <= element.warnThreshold) {
            color = element.colorWarn;
        }

        String labelPrefix = safe(element.label).isBlank() ? "" : (safe(element.label) + ": ");
        String text = labelPrefix + element.prefix + formatValue(value) + element.suffix;
        float scale = Math.max(0.5f, element.fontScale);
        int tx = alignedStartX(x, element, text, element.drawBackground, scale);
        int ty = alignedStartY(y, element, Math.max(9, Math.round(9 * scale)), element.drawBackground);
        drawStyledTextLine(context, text, tx, ty, color, scale);
    }

    private static void renderListElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height, element.backgroundColor);
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        String src = MacroPlaceholders.expand(MinecraftClient.getInstance(), "{" + element.sourceToken + "}");
        if (src == null) {
            src = "";
        }
        List<String> lines = splitListSource(src);
        if (lines.isEmpty()) {
            lines = List.of("(none)");
        }

        int maxLines = Math.max(1, element.maxLines);
        int scroll = Math.max(0, element.listScroll);
        if (scroll >= lines.size()) {
            scroll = Math.max(0, lines.size() - 1);
        }
        int end = Math.min(lines.size(), scroll + maxLines);
        List<String> visible = lines.subList(scroll, end);
        renderLinesElement(context, element, x, y, visible);
    }

    private static void renderShapeElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        String shape = Objects.toString(element.shapeType, "rounded_rect").toLowerCase(Locale.ROOT);
        int w = Math.max(1, element.width);
        int h = Math.max(1, element.height);

        switch (shape) {
            case "line" -> drawLineShape(context, x, y, w, h, element.borderColor, Math.max(1, element.shapeThickness));
            case "circle" -> drawCircleShape(context, x + (w / 2), y + (h / 2), Math.max(1, Math.min(w, h) / 2), element.backgroundColor, element.borderColor, element.shapeFilled, Math.max(1, element.shapeThickness));
            case "rect" -> {
                if (element.shapeFilled || element.drawBackground) {
                    context.fill(x, y, x + w, y + h, element.backgroundColor);
                }
                if (element.drawBorder || !element.shapeFilled) {
                    drawBorder(context, x, y, w, h, element.borderColor);
                }
            }
            default -> drawRoundedRectShape(context, x, y, w, h, Math.max(0, element.shapeRadius), element.backgroundColor, element.borderColor, element.shapeFilled, Math.max(1, element.shapeThickness));
        }
    }

    private static void renderStateBadgeElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        String raw = MacroPlaceholders.expand(MinecraftClient.getInstance(), "{" + element.sourceToken + "}");
        boolean on = parseBoolean(raw);
        int bg = on ? element.colorStart : element.colorEnd;

        context.fill(x, y, x + element.width, y + element.height, bg);
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor);
        }

        String label = safe(element.label).isBlank() ? "STATE" : element.label;
        String value = on ? safe(element.stateOnText) : safe(element.stateOffText);
        String text = element.stateShowValue ? (label + ": " + value) : label;
        int tx = alignedStartX(x, element, text, true, 1.0f);
        int ty = alignedStartY(y, element, 9, true);
        drawStyledTextLine(context, text, tx, ty, element.textColor, 1.0f);
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

    private static int blendColor(int c1, int c2, float t) {
        float tt = Math.clamp(t, 0.0f, 1.0f);
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * tt);
        int r = Math.round(r1 + (r2 - r1) * tt);
        int g = Math.round(g1 + (g2 - g1) * tt);
        int b = Math.round(b1 + (b2 - b1) * tt);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static ItemStack resolveIconStack(MacroHudDataHandler.HudElement element) {
        String kind = safe(element.iconKind).toLowerCase(Locale.ROOT);
        String rawId = safe(element.iconId);
        Identifier id = Identifier.tryParse(rawId);
        if (id == null) {
            return new ItemStack(Items.BARRIER);
        }

        try {
            if ("block".equals(kind)) {
                var block = Registries.BLOCK.get(id);
                return block == null ? new ItemStack(Items.BARRIER) : new ItemStack(block.asItem());
            }
            if ("entity".equals(kind)) {
                Identifier eggId = Identifier.tryParse(id.getNamespace() + ":" + id.getPath() + "_spawn_egg");
                if (eggId != null && Registries.ITEM.containsId(eggId)) {
                    return new ItemStack(Registries.ITEM.get(eggId));
                }
                return new ItemStack(Items.BARRIER);
            }

            var item = Registries.ITEM.get(id);
            if (item == null || item == Items.AIR) {
                return new ItemStack(Items.BARRIER);
            }
            return new ItemStack(item);
        } catch (Exception ignored) {
            return new ItemStack(Items.BARRIER);
        }
    }

    private static float resolveCooldownProgress(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || stack == null || stack.isEmpty()) {
            return 0.0f;
        }
        try {
            var cooldowns = client.player.getItemCooldownManager();
            try {
                var m = cooldowns.getClass().getMethod("getCooldownProgress", stack.getItem().getClass().getSuperclass(), float.class);
                Object value = m.invoke(cooldowns, stack.getItem(), 0.0f);
                if (value instanceof Float f) {
                    return Math.clamp(f, 0.0f, 1.0f);
                }
            } catch (Exception ignored) {
                // fallback below
            }
            try {
                var m = cooldowns.getClass().getMethod("getCooldownProgress", stack.getItem().getClass(), float.class);
                Object value = m.invoke(cooldowns, stack.getItem(), 0.0f);
                if (value instanceof Float f) {
                    return Math.clamp(f, 0.0f, 1.0f);
                }
            } catch (Exception ignored) {
                // no-op
            }
        } catch (Exception ignored) {
            // no-op
        }
        return 0.0f;
    }

    private static Double resolveNumericToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String expanded = MacroPlaceholders.expand(MinecraftClient.getInstance(), "{" + token + "}");
        return parseFirstDouble(expanded);
    }

    private static Double parseFirstDouble(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String cleaned = raw.trim().replace(',', '.');
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception ignored) {
            StringBuilder num = new StringBuilder();
            boolean dotSeen = false;
            for (int i = 0; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if ((c >= '0' && c <= '9') || (num.isEmpty() && (c == '-' || c == '+'))) {
                    num.append(c);
                } else if (c == '.' && !dotSeen) {
                    dotSeen = true;
                    num.append(c);
                } else if (!num.isEmpty()) {
                    break;
                }
            }
            if (!num.isEmpty()) {
                try {
                    return Double.parseDouble(num.toString());
                } catch (Exception ignored2) {
                    return null;
                }
            }
            return null;
        }
    }

    private static String formatValue(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static List<String> splitListSource(String src) {
        if (src == null || src.isBlank()) {
            return new ArrayList<>();
        }
        String normalized = src.replace("\\n", "\n").replace("\r", "");
        List<String> out = new ArrayList<>();
        if (normalized.contains("\n")) {
            for (String line : normalized.split("\\n")) {
                String t = line == null ? "" : line.trim();
                if (!t.isEmpty()) {
                    out.add(t);
                }
            }
            return out;
        }
        for (String part : normalized.split(",")) {
            String t = part == null ? "" : part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static boolean parseBoolean(String raw) {
        if (raw == null) {
            return false;
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        return t.equals("true") || t.equals("1") || t.equals("yes") || t.equals("on") || t.equals("enabled");
    }

    private static void drawRoundedRectShape(DrawContext context, int x, int y, int w, int h, int radius, int fillColor, int borderColor, boolean filled, int thickness) {
        int r = Math.clamp(radius, 0, Math.min(w, h) / 2);
        if (filled) {
            context.fill(x + r, y, x + w - r, y + h, fillColor);
            context.fill(x, y + r, x + w, y + h - r, fillColor);
            fillCircleQuadrants(context, x + r, y + r, r, fillColor, true, 1);
            fillCircleQuadrants(context, x + w - r - 1, y + r, r, fillColor, true, 1);
            fillCircleQuadrants(context, x + r, y + h - r - 1, r, fillColor, true, 1);
            fillCircleQuadrants(context, x + w - r - 1, y + h - r - 1, r, fillColor, true, 1);
        }
        if (!filled || thickness > 0) {
            for (int i = 0; i < thickness; i++) {
                drawBorder(context, x + i, y + i, Math.max(1, w - i * 2), Math.max(1, h - i * 2), borderColor);
            }
        }
    }

    private static void drawCircleShape(DrawContext context, int cx, int cy, int radius, int fillColor, int borderColor, boolean filled, int thickness) {
        int r = Math.max(1, radius);
        if (filled) {
            for (int y = -r; y <= r; y++) {
                int xLen = (int) Math.sqrt(Math.max(0, r * r - y * y));
                context.fill(cx - xLen, cy + y, cx + xLen + 1, cy + y + 1, fillColor);
            }
        }
        for (int t = 0; t < Math.max(1, thickness); t++) {
            int rr = Math.max(1, r - t);
            for (int y = -rr; y <= rr; y++) {
                int xLen = (int) Math.sqrt(Math.max(0, rr * rr - y * y));
                context.fill(cx - xLen, cy + y, cx - xLen + 1, cy + y + 1, borderColor);
                context.fill(cx + xLen, cy + y, cx + xLen + 1, cy + y + 1, borderColor);
            }
        }
    }

    private static void drawLineShape(DrawContext context, int x, int y, int w, int h, int color, int thickness) {
        int x1 = x;
        int y1 = y + h - 1;
        int x2 = x + w - 1;
        int y2 = y;
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            context.fill(x1, y1, x1 + thickness, y1 + thickness, color);
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private static void fillCircleQuadrants(DrawContext context, int cx, int cy, int radius, int color, boolean filled, int thickness) {
        if (radius <= 0) {
            return;
        }
        if (filled) {
            for (int y = -radius; y <= radius; y++) {
                int xLen = (int) Math.sqrt(Math.max(0, radius * radius - y * y));
                context.fill(cx - xLen, cy + y, cx + xLen + 1, cy + y + 1, color);
            }
            return;
        }
        for (int t = 0; t < Math.max(1, thickness); t++) {
            int rr = Math.max(1, radius - t);
            for (int y = -rr; y <= rr; y++) {
                int xLen = (int) Math.sqrt(Math.max(0, rr * rr - y * y));
                context.fill(cx - xLen, cy + y, cx - xLen + 1, cy + y + 1, color);
                context.fill(cx + xLen, cy + y, cx + xLen + 1, cy + y + 1, color);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
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
            case TOP_CENTER, BOTTOM_CENTER, MIDDLE_CENTER, CENTER -> (screenW - element.width) / 2 + element.x;
        };
    }

    private static int resolveY(MacroHudDataHandler.HudElement element, int screenH) {
        return switch (element.anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> element.y;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenH - element.height - element.y;
            case MIDDLE_CENTER, CENTER -> (screenH - element.height) / 2 + element.y;
        };
    }
}

