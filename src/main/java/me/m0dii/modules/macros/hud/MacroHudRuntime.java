package me.m0dii.modules.macros.hud;

import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.modules.macros.MacroPlaceholders;
import me.m0dii.modules.scripting.HudButtonScriptExecutor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
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
        List<MacroHudDataHandler.HudElement> layeredElements = HudElementUtils.sortedByLayer(cfg.elements);
        String hoveredButtonId = interactiveMode ? resolveHoveredButtonId(layeredElements, client, screenW, screenH) : null;

        for (MacroHudDataHandler.HudElement element : layeredElements) {
            if (!element.visible) {
                continue;
            }

            if (!matchesVisibility(element, client.currentScreen)) {
                continue;
            }

            int x = resolveX(element, screenW);
            int y = resolveY(element, screenH);

            switch (element.type) {
                case TEXT -> renderTextElement(context, element, x, y);
                case BUTTON -> renderButtonElement(context, element, x, y, interactiveMode, element.id.equals(hoveredButtonId));
                case MACRO_KEYBINDS -> renderMacroKeybindElement(context, element, x, y);
                case ICON -> renderIconElement(context, element, x, y);
                case BAR -> renderBarElement(context, element, x, y);
                case VALUE -> renderValueElement(context, element, x, y);
                case LIST -> renderListElement(context, element, x, y);
                case INVENTORY -> renderInventoryElement(context, element, x, y);
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
        if (client.player == null || client.currentScreen == null) {
            return false;
        }

        MacroHudDataHandler.HudConfig cfg = MacroHudDataHandler.getConfigCopy();
        if (!cfg.enabled) {
            return false;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        List<MacroHudDataHandler.HudElement> layeredElements = HudElementUtils.sortedByLayer(cfg.elements);
        for (int i = layeredElements.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement element = layeredElements.get(i);
            if (!element.visible || element.type != MacroHudDataHandler.ElementType.BUTTON) {
                continue;
            }
            if (!matchesVisibility(element, client.currentScreen)) {
                continue;
            }
            int x = resolveX(element, screenW);
            int y = resolveY(element, screenH);
            if (!contains(x, y, element, mouseX, mouseY)) {
                continue;
            }

            String actionName = (element.label == null || element.label.isBlank()) ? "HUD Action" : element.label;
            if (element.buttonAction != null && !element.buttonAction.isBlank()) {
                switch (element.buttonExecutionMode) {
                    case COMMAND -> CommandMacros.runInlineAction(actionName, element.buttonAction);
                    case GROOVY_SCRIPT, KOTLIN_SCRIPT, JAVASCRIPT_SCRIPT ->
                            HudButtonScriptExecutor.runScript(actionName, element.buttonAction, element.buttonExecutionMode, element.runScriptsAsync);
                }
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

    private static boolean matchesVisibility(MacroHudDataHandler.HudElement element, Screen screen) {
        MacroHudDataHandler.VisibilityMode mode = element.visibilityMode;
        return switch (mode == null ? MacroHudDataHandler.VisibilityMode.ALWAYS : mode) {
            case ALWAYS -> true;
            case CHAT -> screen instanceof ChatScreen;
            case INVENTORY -> screen instanceof InventoryScreen;
            case CONTAINER -> screen instanceof HandledScreen<?> handled && !(handled instanceof InventoryScreen);
            case CHEST -> screen instanceof GenericContainerScreen;
            case SCREEN -> matchesScreenTypeFilter(screen, element.visibilityScreenType);
        };
    }

    private static boolean matchesScreenTypeFilter(Screen screen, String filter) {
        if (screen == null) {
            return false;
        }
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }
        String simple = screen.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String fqcn = screen.getClass().getName().toLowerCase(Locale.ROOT);
        return simple.contains(needle) || fqcn.contains(needle);
    }

    private static String resolveHoveredButtonId(List<MacroHudDataHandler.HudElement> layeredElements,
                                                 MinecraftClient client,
                                                 int screenW,
                                                 int screenH) {
        int mx = (int) Math.round(client.mouse.getX() * (double) screenW / Math.max(1, client.getWindow().getWidth()));
        int my = (int) Math.round(client.mouse.getY() * (double) screenH / Math.max(1, client.getWindow().getHeight()));
        for (int i = layeredElements.size() - 1; i >= 0; i--) {
            MacroHudDataHandler.HudElement element = layeredElements.get(i);
            if (!element.visible || element.type != MacroHudDataHandler.ElementType.BUTTON) {
                continue;
            }
            if (!matchesVisibility(element, client.currentScreen)) {
                continue;
            }
            int x = resolveX(element, screenW);
            int y = resolveY(element, screenH);
            if (contains(x, y, element, mx, my)) {
                return element.id;
            }
        }
        return null;
    }

    private static void renderTextElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        List<String> lines = splitLines(expanded(element.text));
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
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

    private static void renderButtonElement(DrawContext context,
                                            MacroHudDataHandler.HudElement element,
                                            int x,
                                            int y,
                                            boolean interactiveMode,
                                            boolean hovered) {
        int x1 = x;
        int y1 = y;
        int x2 = x + element.width;
        int y2 = y + element.height;
        List<String> lines = splitLines(expanded(element.label));

        if (element.drawBackground) {
            int bg = interactiveMode ? brighten(element.backgroundColor, hovered ? 0x30303030 : 0x10101010) : element.backgroundColor;
            bg = effectiveBackgroundColor(bg, element.backgroundOpaque, element.backgroundAlpha);
            context.fill(x1, y1, x2, y2, bg);
        }

        if (element.drawBorder) {
            int border = hovered ? brighten(element.borderColor, 0x00181818) : element.borderColor;
            drawBorder(context, x1, y1, element.width, element.height, border, element.borderMode);
        }

        if (hovered) {
            context.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0x60FFFFFF);
            context.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, 0x50000000);
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
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
        }

        if ("entity_model".equalsIgnoreCase(safe(element.iconKind))) {
            drawPlayerModelIcon(context, element, x, y);
            String label = safe(element.label);
            if (!label.isBlank()) {
                float scale = Math.max(0.5f, element.fontScale);
                int tx = alignedStartX(x, element, label, true, scale);
                int ty = y + Math.max(1, element.height - Math.max(9, Math.round(9 * scale)));
                drawStyledTextLine(context, label, tx, ty, element.textColor, scale);
            }
            return;
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
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
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
                    float t = segments == 1 ? progress : (i / (float) (segments - 1));
                    int c = blendColor(element.colorStart, element.colorEnd, t);
                    context.fill(sx, innerY, sx + fillW, innerY + innerH, c);
                }
            }
        } else {
            int fillW = Math.max(0, Math.round(innerW * progress));
            context.fill(innerX, innerY, innerX + innerW, innerY + innerH, 0x33000000);
            for (int px = 0; px < fillW; px++) {
                float t = innerW == 1 ? progress : (px / (float) (innerW - 1));
                int c = blendColor(element.colorStart, element.colorEnd, t);
                context.fill(innerX + px, innerY, innerX + px + 1, innerY + innerH, c);
            }
        }

        String txt = element.prefix + formatValue(value) + element.suffix;
        float scale = Math.max(0.5f, element.fontScale);
        int tx = alignedStartX(x, element, txt, true, scale);
        int ty = alignedStartY(y, element, Math.max(9, Math.round(9 * scale)), true);
        drawStyledTextLine(context, txt, tx, ty, element.textColor, scale);
    }

    private static void renderValueElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
        }

        Double valueToken = resolveNumericToken(element.sourceToken);
        double value = valueToken == null ? 0.0 : valueToken;
        int color = element.textColor;
        if (value <= element.critThreshold) {
            color = element.colorCrit;
        } else if (value <= element.warnThreshold) {
            color = element.colorWarn;
        }

        String prefix = preserve(element.prefix);
        if (prefix.isBlank()) {
            String label = safe(element.label);
            if (!label.isBlank() && !"Value".equalsIgnoreCase(label)) {
                prefix = label + ": ";
            }
        }
        String text = prefix + formatValue(value) + preserve(element.suffix);
        float scale = Math.max(0.5f, element.fontScale);
        int tx = alignedStartX(x, element, text, element.drawBackground, scale);
        int ty = alignedStartY(y, element, Math.max(9, Math.round(9 * scale)), element.drawBackground);
        drawStyledTextLine(context, text, tx, ty, color, scale);
    }

    private static void renderListElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
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

    private static void renderInventoryElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        MacroInventoryWidgetSupport.render(
                context,
                client.textRenderer,
                element,
                client.player,
                x,
                y,
                element.width,
                element.height
        );
    }

    private static void renderShapeElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        String shape = Objects.toString(element.shapeType, "rounded_rect").toLowerCase(Locale.ROOT);
        int w = Math.max(1, element.width);
        int h = Math.max(1, element.height);

        switch (shape) {
            case "line" -> drawLineShape(context, x, y, w, h,
                    effectiveBackgroundColor(element.drawBorder ? element.borderColor : element.backgroundColor,
                            element.backgroundOpaque, element.backgroundAlpha),
                    Math.max(1, element.shapeThickness),
                    element.drawBorder || element.shapeFilled);
            case "cross" -> drawCrossShape(context, x, y, w, h,
                    effectiveBackgroundColor(element.drawBorder ? element.borderColor : element.backgroundColor,
                            element.backgroundOpaque, element.backgroundAlpha),
                    Math.max(1, element.shapeThickness),
                    element.drawBorder || element.shapeFilled);
            case "triangle" -> drawTriangleShape(context, x, y, w, h,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha),
                    effectiveBackgroundColor(element.borderColor, element.backgroundOpaque, element.backgroundAlpha),
                    element.shapeFilled || element.drawBackground,
                    element.drawBorder,
                    Math.max(1, element.shapeThickness));
            case "diamond" -> drawDiamondShape(context, x, y, w, h,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha),
                    effectiveBackgroundColor(element.borderColor, element.backgroundOpaque, element.backgroundAlpha),
                    element.shapeFilled || element.drawBackground,
                    element.drawBorder,
                    Math.max(1, element.shapeThickness));
            case "circle" -> drawCircleShape(context, x + (w / 2), y + (h / 2), Math.max(1, Math.min(w, h) / 2),
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha),
                    effectiveBackgroundColor(element.borderColor, element.backgroundOpaque, element.backgroundAlpha),
                    element.shapeFilled || element.drawBackground,
                    element.drawBorder,
                    Math.max(1, element.shapeThickness));
            case "rect" -> {
                if (element.shapeFilled || element.drawBackground) {
                    context.fill(x, y, x + w, y + h,
                            effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
                }
                if (element.drawBorder) {
                    drawBorder(context, x, y, w, h, element.borderColor, element.borderMode);
                }
            }
            default -> drawRoundedRectShape(context, x, y, w, h, Math.max(0, element.shapeRadius),
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha),
                    effectiveBackgroundColor(element.borderColor, element.backgroundOpaque, element.backgroundAlpha),
                    element.shapeFilled || element.drawBackground,
                    element.drawBorder,
                    Math.max(1, element.shapeThickness));
        }
    }

    private static void renderStateBadgeElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        String raw = MacroPlaceholders.expand(MinecraftClient.getInstance(), "{" + element.sourceToken + "}");
        boolean on = resolveStateBoolean(raw, element);
        int bg = effectiveBackgroundColor(on ? element.colorStart : element.colorEnd, element.backgroundOpaque, element.backgroundAlpha);

        context.fill(x, y, x + element.width, y + element.height, bg);
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
        }

        String label = safe(element.label).isBlank() ? "STATE" : element.label;
        String value = on ? safe(element.stateOnText) : safe(element.stateOffText);
        String text = element.stateShowValue ? (label + ": " + value) : label;
        float scale = Math.max(0.5f, element.fontScale);
        int tx = alignedStartX(x, element, text, true, scale);
        int ty = alignedStartY(y, element, Math.max(9, Math.round(9 * scale)), true);
        drawStyledTextLine(context, text, tx, ty, element.textColor, scale);
    }

    private static void renderLinesElement(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y, List<String> lines) {
        if (element.drawBackground) {
            context.fill(x, y, x + element.width, y + element.height,
                    effectiveBackgroundColor(element.backgroundColor, element.backgroundOpaque, element.backgroundAlpha));
        }
        if (element.drawBorder) {
            drawBorder(context, x, y, element.width, element.height, element.borderColor, element.borderMode);
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

    private static void drawBorder(DrawContext context, int x, int y, int width, int height, int color, MacroHudDataHandler.BorderMode mode) {
        int x2 = x + width;
        int y2 = y + height;
        MacroHudDataHandler.BorderMode resolved = mode == null ? MacroHudDataHandler.BorderMode.FULL : mode;
        if (resolved == MacroHudDataHandler.BorderMode.FULL || resolved == MacroHudDataHandler.BorderMode.TOP) {
            context.fill(x, y, x2, y + 1, color);
        }
        if (resolved == MacroHudDataHandler.BorderMode.FULL || resolved == MacroHudDataHandler.BorderMode.BOTTOM) {
            context.fill(x, y2 - 1, x2, y2, color);
        }
        if (resolved == MacroHudDataHandler.BorderMode.FULL || resolved == MacroHudDataHandler.BorderMode.LEFT) {
            context.fill(x, y, x + 1, y2, color);
        }
        if (resolved == MacroHudDataHandler.BorderMode.FULL || resolved == MacroHudDataHandler.BorderMode.RIGHT) {
            context.fill(x2 - 1, y, x2, y2, color);
        }
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

    private static int effectiveBackgroundColor(int color, boolean forceOpaque, int alphaOverride) {
        int alpha = Math.clamp(alphaOverride, 0, 255);
        int withAlpha = (alpha << 24) | (color & 0x00FFFFFF);
        if (forceOpaque) {
            return 0xFF000000 | (withAlpha & 0x00FFFFFF);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen<?> handled && !(handled instanceof InventoryScreen)) {
            // Reduce alpha and lift brightness so HUD remains readable above container dim overlays.
            int targetAlpha = Math.clamp(alpha, 0x66, 0xA0);
            int brightened = brighten(withAlpha, 0x14141414);
            return (targetAlpha << 24) | (brightened & 0x00FFFFFF);
        }
        return withAlpha;
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
                if (Registries.ITEM.containsId(id)) {
                    var directItem = Registries.ITEM.get(id);
                    if (directItem != null && directItem != Items.AIR) {
                        return new ItemStack(directItem);
                    }
                }
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

    private static void drawPlayerModelIcon(DrawContext context, MacroHudDataHandler.HudElement element, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity target = resolveModelTargetEntity(client, element);
        if (target == null) {
            return;
        }
        int w = Math.max(1, element.width);
        int h = Math.max(1, element.height);
        int innerPad = element.drawBorder ? 2 : 1;
        int boxW = Math.max(1, w - (innerPad * 2));
        int boxH = Math.max(1, h - (innerPad * 2));
        int baseSize = element.modelAutoFit
                ? Math.max(8, Math.round(Math.min(boxW, boxH) * 0.48f))
                : Math.max(8, Math.min(w, h) - 4);
        int size = Math.max(8, Math.round(baseSize * Math.clamp(element.modelZoom, 0.2f, 2.5f)));
        int left = x + innerPad + element.modelOffsetX;
        int top = y + innerPad + element.modelOffsetY;
        int right = x + w - innerPad + element.modelOffsetX;
        int bottom = y + h - innerPad + element.modelOffsetY;
        if (right <= left) {
            right = left + 1;
        }
        if (bottom <= top) {
            bottom = top + 1;
        }
        drawEntityModelReflective(context, target, left, top, right, bottom, size,
                element.modelYaw, element.modelPitch, element.modelFollowLook);
    }

    private static Entity resolveModelTargetEntity(MinecraftClient client, MacroHudDataHandler.HudElement element) {
        if (client == null) {
            return null;
        }
        String id = safe(element == null ? null : element.iconId).toLowerCase(Locale.ROOT);
        // Explicitly map missing/"player" model ids to local player.
        if (id.isBlank() || "player".equals(id) || "minecraft:player".equals(id)) {
            return client.player;
        }
        return client.player != null ? client.player : client.getCameraEntity();
    }

    private static void drawEntityModelReflective(DrawContext context, Entity entity,
                                                  int left, int top, int right, int bottom,
                                                  int size, float yaw, float pitch, boolean followLook) {
        if (entity == null || size < 1) {
            return;
        }
        try {
            Class<?> inventoryScreen = Class.forName("net.minecraft.client.gui.screen.ingame.InventoryScreen");
            Class<?> drawContextClass = Class.forName("net.minecraft.client.gui.DrawContext");
            Class<?> entityClass = Class.forName("net.minecraft.entity.Entity");
            Class<?> vectorClass = Class.forName("org.joml.Vector3f");
            Class<?> quaternionClass = Class.forName("org.joml.Quaternionf");
            Class<?> livingEntityClass = Class.forName("net.minecraft.entity.LivingEntity");

            float[] resolvedAngles = resolveModelAngles(entity, yaw, pitch, followLook);
            float resolvedYaw = resolvedAngles[0];
            float resolvedPitch = resolvedAngles[1];

            Object vecZero = vectorClass.getConstructor(float.class, float.class, float.class).newInstance(0.0f, 0.0f, 0.0f);
            Object modelQuat = buildModelQuaternion(quaternionClass, resolvedYaw, resolvedPitch);
            Object identityQuat = quaternionClass.getConstructor().newInstance();

            if (invokePreferredEntityDraw(inventoryScreen, context, entity,
                    left, top, right, bottom, size,
                    vecZero, modelQuat, identityQuat,
                    drawContextClass, vectorClass, quaternionClass, entityClass, livingEntityClass,
                    resolvedYaw, resolvedPitch)) {
                return;
            }

            List<Class<?>> owners = List.of(inventoryScreen, drawContextClass);
            for (Class<?> owner : owners) {
                for (var method : owner.getMethods()) {
                    if (!"drawEntity".equals(method.getName())) {
                        continue;
                    }
                    boolean staticMethod = java.lang.reflect.Modifier.isStatic(method.getModifiers());
                    if (!staticMethod && !drawContextClass.isAssignableFrom(owner)) {
                        continue;
                    }

                    Class<?>[] paramTypes = method.getParameterTypes();
                    int intParams = 0;
                    for (Class<?> type : paramTypes) {
                        if (type == int.class || type == Integer.class) {
                            intParams++;
                        }
                    }
                    if (intParams < 3) {
                        continue;
                    }

                    int[] intArgValues = buildEntityDrawIntArgs(left, top, right, bottom, size, intParams);
                    if (intArgValues == null) {
                        continue;
                    }

                    // Compute logical centre from the int args for sensible float defaults.
                    float fallbackCx = (intArgValues.length >= 2)
                            ? (intArgValues[0] + (intArgValues.length >= 4 ? intArgValues[2] : intArgValues[0])) / 2.0f
                            : 0.0f;
                    float fallbackCy = (intArgValues.length >= 2)
                            ? (intArgValues[1] + (intArgValues.length >= 4 ? intArgValues[3] : intArgValues[1])) / 2.0f
                            : 0.0f;
                    // For mouse-based APIs: mouseX = cx prevents any rotation (entity faces camera).
                    // We then rely on entity.bodyYaw for the desired angle.
                    float mouseX = fallbackCx - 40.0f * (float) Math.tan(Math.clamp(resolvedYaw, -30f, 30f) / 20.0f);
                    float mouseY = fallbackCy + 40.0f * (float) Math.tan(Math.clamp(resolvedPitch, -30f, 30f) / 20.0f);

                    Object[] args = new Object[paramTypes.length];
                    int intArg = 0;
                    int quatArg = 0;
                    int floatArg = 0;
                    boolean accepted = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        Class<?> type = paramTypes[i];
                        if (drawContextClass.isAssignableFrom(type)) {
                            args[i] = context;
                            continue;
                        }
                        if (type == int.class || type == Integer.class) {
                            args[i] = intArgValues[Math.min(intArg++, intArgValues.length - 1)];
                            continue;
                        }
                        if (type == float.class || type == Float.class) {
                            // Assign: f-offset=0.0625f, then mouseX, mouseY for mouse-based APIs.
                            switch (floatArg++) {
                                case 0 -> args[i] = 0.0625f;
                                case 1 -> args[i] = mouseX;
                                case 2 -> args[i] = mouseY;
                                default -> args[i] = 0.0f;
                            }
                            continue;
                        }
                        if (type == double.class || type == Double.class) {
                            args[i] = 0.0d;
                            continue;
                        }
                        if (type == boolean.class || type == Boolean.class) {
                            args[i] = false;
                            continue;
                        }
                        if (type.getName().equals("org.joml.Vector3f")) {
                            args[i] = vecZero;
                            continue;
                        }
                        if (type.getName().equals("org.joml.Quaternionf")) {
                            args[i] = (quatArg++ == 0) ? modelQuat : identityQuat;
                            continue;
                        }
                        if (entityClass.isAssignableFrom(type)) {
                            args[i] = entity;
                            continue;
                        }
                        accepted = false;
                        break;
                    }
                    if (!accepted) {
                        continue;
                    }
                    // For quaternion-based paths the entity orientation drives rotation.
                    // For mouse-float paths the MC API sets bodyYaw from mouse coords internally,
                    // so applyEntityOrientationForScreen is still useful for the 8-param variant.
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(staticMethod ? null : context, args);
                        return;
                    } catch (Throwable ignoredInvokeFailure) {
                        // Try other candidate signatures before giving up.
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }
            }
        } catch (Exception ignored) {
            // Silent fallback when drawEntity signatures differ across versions.
        }
    }

    private static boolean invokePreferredEntityDraw(Class<?> inventoryScreen,
                                                     DrawContext context,
                                                     Entity entity,
                                                     int left, int top, int right, int bottom, int size,
                                                     Object vecZero, Object modelQuat, Object identityQuat,
                                                     Class<?> drawContextClass, Class<?> vectorClass, Class<?> quaternionClass,
                                                     Class<?> entityClass, Class<?> livingEntityClass,
                                                     float resolvedYaw, float resolvedPitch) {
        int cx = left + ((right - left) / 2);
        int cy = bottom;

        // Collect all static drawEntity methods so we can prioritise.
        java.util.List<Method> candidates = new java.util.ArrayList<>();
        for (Method m : inventoryScreen.getMethods()) {
            if ("drawEntity".equals(m.getName()) && java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                candidates.add(m);
            }
        }

        // ── Priority 1: MC 1.21.x low-level 8-param overload ───────────────────────
        // drawEntity(DrawContext, float x, float y, float size,
        //            Vector3f, Quaternionf, @Nullable Quaternionf, LivingEntity)
        // This overload allows full 360° rotation.
        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            if (!matchesFloatXYSizeVecQuatSignature(p, drawContextClass, vectorClass, quaternionClass, livingEntityClass)) {
                continue;
            }
            if (!p[7].isInstance(entity)) {
                continue;
            }
            try {
                float entityScale = (entity instanceof LivingEntity le) ? le.getScale() : 1.0f;
                float q = (float) size / entityScale;
                // MC convention: bodyYaw=180 means entity faces the camera after the renderer's
                // built-in coordinate flip.  resolvedYaw is the offset from "facing camera".
                float mcYaw = 180.0f + resolvedYaw;
                // Quaternion: Z-flip (faces camera) then X-pitch correction for the render matrix.
                // entity.setPitch drives head/body pitch; the quaternion pitch corrects the
                // lighting direction (matches what the 10-param overload builds internally).
                float pitchRad = (float) (-resolvedPitch * Math.PI / 180.0);
                Object uiQuat = quaternionClass.getConstructor().newInstance();
                Method rotZ = quaternionClass.getMethod("rotateZ", float.class);
                Method rotX = quaternionClass.getMethod("rotateX", float.class);
                rotZ.invoke(uiQuat, (float) Math.PI);
                rotX.invoke(uiQuat, pitchRad);
                Object lightQuat = quaternionClass.getConstructor().newInstance();
                rotX.invoke(lightQuat, pitchRad);
                Object vec = vectorClass.getConstructor(float.class, float.class, float.class)
                        .newInstance(0.0f, entity.getHeight() / 2.0f + 0.0625f * entityScale, 0.0f);
                float fcx = (left + right) / 2.0f;
                float fcy = (top + bottom) / 2.0f;
                EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                applyEntityOrientationForScreen(entity, mcYaw, resolvedPitch);
                try {
                    method.invoke(null, context, fcx, fcy, q, vec, uiQuat, lightQuat, entity);
                    return true;
                } finally {
                    restoreEntityOrientation(entity, snapshot);
                }
            } catch (Throwable ignored) {
                // fall through to next candidate
            }
        }

        // ── Priority 2: MC 1.21.x primary 10-param overload ────────────────────────
        // drawEntity(DrawContext, int x1, int y1, int x2, int y2, int size,
        //            float f, float mouseX, float mouseY, LivingEntity)
        // The API computes bodyYaw from the mouse position internally.
        // Formula (derived from MC source):
        //   i = atan((centerX - mouseX) / 40)   [radians]
        //   entity.bodyYaw = 180 + i * 20        [degrees]
        // → mouseX = centerX - 40 * tan(resolvedYaw / 20)   (limited to ±~31°)
        // Similar for pitch:
        //   j = atan((centerY - mouseY) / 40)
        //   entity.pitch = -j * 20
        // → mouseY = centerY + 40 * tan(resolvedPitch / 20)
        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            if (!matchesRectMouseFloat3Signature(p, drawContextClass, livingEntityClass)) {
                continue;
            }
            if (!p[9].isInstance(entity)) {
                continue;
            }
            try {
                float fcx = (left + right) / 2.0f;
                float fcy = (top + bottom) / 2.0f;
                // Clamp to the safe range of the mouse-based API (max |yaw/20| < π/2).
                float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
                float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
                float mx = fcx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
                float my = fcy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
                method.invoke(null, context, left, top, right, bottom, size, 0.0625f, mx, my, entity);
                return true;
            } catch (Throwable ignored) {
                // fall through
            }
        }

        // ── Priority 3: legacy quaternion-based APIs (pre-1.20.5) ───────────────────
        for (Method method : candidates) {
            Class<?>[] p = method.getParameterTypes();
            try {
                if (matchesRectQuatSignature(p, drawContextClass, vectorClass, quaternionClass)
                        && p[9].isInstance(entity)) {
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(null, context, left, top, right, bottom, size, vecZero, modelQuat, identityQuat, entity);
                        return true;
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }

                if (matchesCenterQuatSignature(p, drawContextClass, quaternionClass, entityClass)
                        && p[6].isInstance(entity)) {
                    EntityOrientationSnapshot snapshot = captureEntityOrientation(entity);
                    applyEntityOrientationForScreen(entity, 180.0f + resolvedYaw, resolvedPitch);
                    try {
                        method.invoke(null, context, cx, cy, size, modelQuat, identityQuat, entity);
                        return true;
                    } finally {
                        restoreEntityOrientation(entity, snapshot);
                    }
                }

                // Very old API: drawEntity(DrawContext, int x, int y, int size, float mouseX, float mouseY, Entity)
                // bodyYaw = 180 + atan((x - mouseX)/40) * 20  →  mouseX = x - 40*tan(yaw/20)
                if (matchesMouseFloatSignature(p, drawContextClass, entityClass)
                        && p[6].isInstance(entity)) {
                    float clampedYaw = Math.clamp(resolvedYaw, -30.0f, 30.0f);
                    float clampedPitch = Math.clamp(resolvedPitch, -30.0f, 30.0f);
                    float mx = cx - 40.0f * (float) Math.tan(clampedYaw / 20.0f);
                    float my = cy + 40.0f * (float) Math.tan(clampedPitch / 20.0f);
                    method.invoke(null, context, cx, cy, size, mx, my, entity);
                    return true;
                }
            } catch (Throwable ignored) {
                // Continue trying other known signatures.
            }
        }
        return false;
    }

    // ── MC 1.21.x primary overload ──────────────────────────────────────────────
    // (DrawContext, int x1, int y1, int x2, int y2, int size, float f, float mouseX, float mouseY, LivingEntity)
    private static boolean matchesRectMouseFloat3Signature(Class<?>[] p,
                                                           Class<?> drawContextClass,
                                                           Class<?> livingEntityClass) {
        return p.length == 10
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && p[4] == int.class && p[5] == int.class
                && p[6] == float.class && p[7] == float.class && p[8] == float.class
                && livingEntityClass.isAssignableFrom(p[9]);
    }

    // ── MC 1.21.x low-level 8-param overload ────────────────────────────────────
    // (DrawContext, float x, float y, float size, Vector3f, Quaternionf, @Nullable Quaternionf, LivingEntity)
    private static boolean matchesFloatXYSizeVecQuatSignature(Class<?>[] p,
                                                              Class<?> drawContextClass,
                                                              Class<?> vectorClass,
                                                              Class<?> quaternionClass,
                                                              Class<?> livingEntityClass) {
        return p.length == 8
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == float.class && p[2] == float.class && p[3] == float.class
                && vectorClass.getName().equals(p[4].getName())
                && quaternionClass.getName().equals(p[5].getName())
                && (quaternionClass.getName().equals(p[6].getName()) || !p[6].isPrimitive())
                && livingEntityClass.isAssignableFrom(p[7]);
    }

    // ── Legacy quaternion overloads (pre-1.20.5 MC) ─────────────────────────────
    private static boolean matchesRectQuatSignature(Class<?>[] p,
                                                    Class<?> drawContextClass,
                                                    Class<?> vectorClass,
                                                    Class<?> quaternionClass) {
        return p.length == 10
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class && p[4] == int.class && p[5] == int.class
                && vectorClass.getName().equals(p[6].getName())
                && quaternionClass.getName().equals(p[7].getName())
                && quaternionClass.getName().equals(p[8].getName());
    }

    private static boolean matchesCenterQuatSignature(Class<?>[] p,
                                                      Class<?> drawContextClass,
                                                      Class<?> quaternionClass,
                                                      Class<?> entityClass) {
        return p.length == 7
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && quaternionClass.getName().equals(p[4].getName())
                && quaternionClass.getName().equals(p[5].getName())
                && entityClass.isAssignableFrom(p[6]);
    }

    private static boolean matchesMouseFloatSignature(Class<?>[] p,
                                                      Class<?> drawContextClass,
                                                      Class<?> entityClass) {
        return p.length == 7
                && drawContextClass.isAssignableFrom(p[0])
                && p[1] == int.class && p[2] == int.class && p[3] == int.class
                && p[4] == float.class && p[5] == float.class
                && entityClass.isAssignableFrom(p[6]);
    }

    private static EntityOrientationSnapshot captureEntityOrientation(Entity entity) {
        if (entity == null) {
            return new EntityOrientationSnapshot(0.0f, 0.0f, null, null, null);
        }
        Float bodyYaw = null;
        Float headYaw = null;
        Float prevHeadYaw = null;
        if (entity instanceof LivingEntity living) {
            bodyYaw = living.getBodyYaw();
            headYaw = living.getHeadYaw();
            prevHeadYaw = getFieldFloat(LivingEntity.class, living, "prevHeadYaw");
        }
        return new EntityOrientationSnapshot(entity.getYaw(), entity.getPitch(), bodyYaw, headYaw, prevHeadYaw);
    }

    /**
     * Sets entity orientation using raw yaw/pitch values (as stored in entity fields).
     */
    private static void applyEntityOrientation(Entity entity, float yaw, float pitch) {
        if (entity == null) {
            return;
        }
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        if (entity instanceof LivingEntity living) {
            living.setBodyYaw(yaw);
            living.setHeadYaw(yaw);
            setFieldFloat(LivingEntity.class, living, "prevHeadYaw", yaw);
        }
    }

    /**
     * Sets entity orientation for InventoryScreen rendering convention.
     * bodyYaw=180 → entity faces the camera; displayYaw should be 180 + user-yaw-offset.
     */
    private static void applyEntityOrientationForScreen(Entity entity, float displayYaw, float pitch) {
        if (entity == null) {
            return;
        }
        entity.setYaw(displayYaw);
        entity.setPitch(pitch);
        if (entity instanceof LivingEntity living) {
            living.setBodyYaw(displayYaw);
            living.setHeadYaw(displayYaw);
            setFieldFloat(LivingEntity.class, living, "prevHeadYaw", displayYaw);
        }
    }

    private static void restoreEntityOrientation(Entity entity, EntityOrientationSnapshot snapshot) {
        if (entity == null || snapshot == null) {
            return;
        }
        entity.setYaw(snapshot.yaw());
        entity.setPitch(snapshot.pitch());
        if (entity instanceof LivingEntity living) {
            if (snapshot.bodyYaw() != null) {
                living.setBodyYaw(snapshot.bodyYaw());
            }
            if (snapshot.headYaw() != null) {
                living.setHeadYaw(snapshot.headYaw());
            }
            if (snapshot.prevHeadYaw() != null) {
                setFieldFloat(LivingEntity.class, living, "prevHeadYaw", snapshot.prevHeadYaw());
            }
        }
    }

    /**
     * Reads a float field by name, traversing the class hierarchy. Returns null if not found.
     */
    private static Float getFieldFloat(Class<?> clazz, Object obj, String fieldName) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.getFloat(obj);
            } catch (NoSuchFieldException ignored) {
                // Try parent
            } catch (Exception ignored) {
                break;
            }
        }
        return null;
    }

    /**
     * Sets a float field by name, traversing the class hierarchy. Silently ignores failures.
     */
    private static void setFieldFloat(Class<?> clazz, Object obj, String fieldName, float value) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.setFloat(obj, value);
                return;
            } catch (NoSuchFieldException ignored) {
                // Try parent
            } catch (Exception ignored) {
                break;
            }
        }
    }

    private record EntityOrientationSnapshot(float yaw, float pitch, Float bodyYaw, Float headYaw, Float prevHeadYaw) {
    }

    private static float[] resolveModelAngles(Entity entity, float yawOffset, float pitchOffset, boolean followLook) {
        float baseYaw = 0.0f;
        float basePitch = 0.0f;
        if (followLook && entity != null) {
            baseYaw = entity instanceof LivingEntity living ? living.getHeadYaw() : entity.getYaw();
            basePitch = entity.getPitch();
        }
        float resolvedYaw = wrapDegrees(baseYaw + yawOffset);
        float resolvedPitch = Math.clamp(basePitch + pitchOffset, -90.0f, 90.0f);
        return new float[]{resolvedYaw, resolvedPitch};
    }


    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    private static Object buildModelQuaternion(Class<?> quaternionClass, float yaw, float pitch) {
        try {
            // Build: rotateZ(π) * rotateX(pitchRad) — matches what MC's first drawEntity overload
            // constructs internally ( quaternionf = rotateZ(π), then mul(rotateX(j*20*π/180)) ).
            // Yaw is applied via entity.bodyYaw/headYaw, NOT via quaternion, to avoid double-rotation.
            Object quat = quaternionClass.getConstructor().newInstance();
            try {
                Method rotateZ = quaternionClass.getMethod("rotateZ", float.class);
                rotateZ.invoke(quat, (float) Math.PI);
            } catch (Exception ignored) {
            }
            float pitchRad = (float) (-pitch * Math.PI / 180.0);
            try {
                Method rotateX = quaternionClass.getMethod("rotateX", float.class);
                rotateX.invoke(quat, pitchRad);
            } catch (Exception ignored) {
            }
            return quat;
        } catch (Exception ignored) {
            try {
                return quaternionClass.getConstructor().newInstance();
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private static int[] buildEntityDrawIntArgs(int left, int top, int right, int bottom, int size, int intParams) {
        int safeSize = Math.max(8, size);
        int safeLeft = left;
        int safeTop = top;
        int safeRight = Math.max(right, safeLeft + 1);
        int safeBottom = Math.max(bottom, safeTop + 1);
        if (intParams == 3) {
            int cx = safeLeft + ((safeRight - safeLeft) / 2);
            int cy = safeBottom;
            return new int[]{cx, cy, safeSize};
        }
        if (intParams >= 5) {
            int[] args = new int[intParams];
            args[0] = safeLeft;
            args[1] = safeTop;
            args[2] = safeRight;
            args[3] = safeBottom;
            args[4] = safeSize;
            for (int i = 5; i < intParams; i++) {
                args[i] = safeSize;
            }
            return args;
        }
        // Skip ambiguous 4-int signatures instead of risking invalid x2/y2 bounds.
        return null;
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

    private static boolean resolveStateBoolean(String raw, MacroHudDataHandler.HudElement element) {
        String t = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!t.isEmpty()) {
            for (String token : splitCsv(element.stateTrueValues)) {
                if (t.equals(token)) {
                    return true;
                }
            }
            for (String token : splitCsv(element.stateFalseValues)) {
                if (t.equals(token)) {
                    return false;
                }
            }
        }
        return parseBoolean(raw);
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String token = part == null ? "" : part.trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                out.add(token);
            }
        }
        return out;
    }

    private static void drawRoundedRectShape(DrawContext context, int x, int y, int w, int h, int radius,
                                             int fillColor, int borderColor, boolean filled,
                                             boolean drawBorder, int thickness) {
        int r = Math.clamp(radius, 0, Math.min(w, h) / 2);
        if (filled) {
            context.fill(x + r, y, x + w - r, y + h, fillColor);
            context.fill(x, y + r, x + w, y + h - r, fillColor);
            fillCircleQuadrants(context, x + r, y + r, r, fillColor, true, 1);
            fillCircleQuadrants(context, x + w - r - 1, y + r, r, fillColor, true, 1);
            fillCircleQuadrants(context, x + r, y + h - r - 1, r, fillColor, true, 1);
            fillCircleQuadrants(context, x + w - r - 1, y + h - r - 1, r, fillColor, true, 1);
        }
        if (drawBorder) {
            for (int i = 0; i < thickness; i++) {
                drawBorder(context, x + i, y + i, Math.max(1, w - i * 2), Math.max(1, h - i * 2), borderColor, MacroHudDataHandler.BorderMode.FULL);
            }
        }
    }

    private static void drawCircleShape(DrawContext context, int cx, int cy, int radius,
                                        int fillColor, int borderColor, boolean filled,
                                        boolean drawBorder, int thickness) {
        int r = Math.max(1, radius);
        if (filled) {
            for (int y = -r; y <= r; y++) {
                int xLen = (int) Math.sqrt(Math.max(0, r * r - y * y));
                context.fill(cx - xLen, cy + y, cx + xLen + 1, cy + y + 1, fillColor);
            }
        }
        if (drawBorder) {
            for (int t = 0; t < Math.max(1, thickness); t++) {
                int rr = Math.max(1, r - t);
                for (int y = -rr; y <= rr; y++) {
                    int xLen = (int) Math.sqrt(Math.max(0, rr * rr - y * y));
                    context.fill(cx - xLen, cy + y, cx - xLen + 1, cy + y + 1, borderColor);
                    context.fill(cx + xLen, cy + y, cx + xLen + 1, cy + y + 1, borderColor);
                }
            }
        }
    }

    private static void drawLineShape(DrawContext context, int x, int y, int w, int h, int color, int thickness, boolean visible) {
        if (!visible) {
            return;
        }
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

    private static void drawCrossShape(DrawContext context, int x, int y, int w, int h, int color, int thickness, boolean visible) {
        if (!visible) {
            return;
        }
        int cx = x + (w / 2);
        int cy = y + (h / 2);
        int half = Math.max(1, Math.min(w, h) / 2);
        context.fill(cx - thickness / 2, y, cx - thickness / 2 + thickness, y + h, color);
        context.fill(x, cy - thickness / 2, x + w, cy - thickness / 2 + thickness, color);
        // Keep center crisp on small widgets.
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
    }

    private static void drawTriangleShape(DrawContext context, int x, int y, int w, int h,
                                          int fillColor, int borderColor, boolean filled,
                                          boolean drawBorder, int thickness) {
        int apexX = x + w / 2;
        int apexY = y;
        int leftX = x;
        int rightX = x + w - 1;
        int baseY = y + h - 1;
        for (int row = 0; row < h; row++) {
            float t = h == 1 ? 1.0f : (row / (float) (h - 1));
            int rowHalfW = Math.max(0, Math.round((w / 2f) * t));
            int yy = apexY + row;
            int rowLeft = apexX - rowHalfW;
            int rowRight = apexX + rowHalfW;
            if (filled) {
                context.fill(rowLeft, yy, rowRight + 1, yy + 1, fillColor);
            }
            if (drawBorder) {
                context.fill(rowLeft, yy, rowLeft + 1, yy + 1, borderColor);
                context.fill(rowRight, yy, rowRight + 1, yy + 1, borderColor);
            }
        }
        if (drawBorder) {
            for (int i = 0; i < thickness; i++) {
                context.fill(leftX + i, baseY - i, rightX - i + 1, baseY - i + 1, borderColor);
            }
        }
    }

    private static void drawDiamondShape(DrawContext context, int x, int y, int w, int h,
                                         int fillColor, int borderColor, boolean filled,
                                         boolean drawBorder, int thickness) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        int ry = Math.max(1, h / 2);
        for (int row = -ry; row <= ry; row++) {
            float t = 1.0f - (Math.abs(row) / (float) ry);
            int halfW = Math.max(0, Math.round((w / 2f) * t));
            int yy = cy + row;
            int left = cx - halfW;
            int right = cx + halfW;
            if (filled) {
                context.fill(left, yy, right + 1, yy + 1, fillColor);
            }
            if (drawBorder) {
                context.fill(left, yy, left + 1, yy + 1, borderColor);
                context.fill(right, yy, right + 1, yy + 1, borderColor);
            }
        }
        if (drawBorder) {
            for (int i = 1; i < Math.max(1, thickness); i++) {
                context.fill(cx - i, y + i, cx + i + 1, y + i + 1, borderColor);
                context.fill(cx - i, y + h - i - 1, cx + i + 1, y + h - i, borderColor);
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

    private static String preserve(String value) {
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
            case TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT -> element.x;
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> screenW - element.width - element.x;
            case TOP_CENTER, BOTTOM_CENTER, MIDDLE_CENTER, CENTER -> (screenW - element.width) / 2 + element.x;
        };
    }

    private static int resolveY(MacroHudDataHandler.HudElement element, int screenH) {
        return switch (element.anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> element.y;
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, CENTER -> (screenH - element.height) / 2 + element.y;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenH - element.height - element.y;
        };
    }
}

