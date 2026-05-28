package me.m0dii.modules.macros.gui;

import me.m0dii.modules.Module;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MacroKeybindOverlayModule extends Module {

    public static final MacroKeybindOverlayModule INSTANCE = new MacroKeybindOverlayModule();

    private MacroKeybindOverlayModule() {
        super("macro_keybind_overlay", "Macro Keybind Overlay", true);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled() || !ModConfig.showMacroKeybindOverlay || isClientNull()) {
            return;
        }

        HudCanvasDataHandler.HudCanvasElement layout = HudCanvasDataHandler.getMutableElement(
                HudCanvasDataHandler.ELEMENT_MACRO_KEYBINDS,
                MacroKeybindOverlayModule::defaultHudCanvasLayout
        );
        if (!layout.visible) {
            return;
        }

        List<String> lines = getInfoLines();
        if (lines.isEmpty()) {
            return;
        }

        TextRenderer tr = client.textRenderer;

        float scale = layout.fontScale;
        if (Float.isNaN(scale) || scale <= 0f) {
            scale = 0.85f;
        }
        scale = Math.clamp(scale, 0.5f, 3.0f);

        final int lineH = Math.max(6, layout.lineHeight);
        final int pad = Math.max(0, layout.padding);

        final int panelW = Math.max(40, layout.width);
        final int panelH = Math.max(20, layout.height);

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int panelX = resolvePanelX(layout, panelW, screenW);
        int panelY = resolvePanelY(layout, panelH, screenH);

        if (layout.drawBackground) {
            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, layout.backgroundColor);
        }
        if (layout.drawBorder) {
            int border = layout.borderColor;
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, border);
            ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, border);
            ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, border);
            ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, border);
        }

        int innerW = Math.max(1, panelW - (pad * 2));
        int logicalInnerW = Math.max(1, (int) Math.floor(innerW / scale));
        int logicalInnerH = Math.max(1, (int) Math.floor(Math.max(1, panelH - (pad * 2)) / scale));
        int maxLines = Math.max(1, logicalInnerH / lineH);

        List<String> drawLines = new ArrayList<>();
        drawLines.add(trimTextToWidth(tr, "Macro Keybinds", logicalInnerW));
        for (String s : lines) {
            if (drawLines.size() >= maxLines) {
                break;
            }
            drawLines.add(trimTextToWidth(tr, s, logicalInnerW));
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

        int textBlockHeight = drawLines.size() * lineH;
        int textX = alignedTextX(layout, tr, drawLines, logicalInnerW);
        int textY = alignedTextY(layout, logicalInnerH, textBlockHeight);

        final int baseX = Math.round(panelX / scale) + pad + textX;
        int y = Math.round(panelY / scale) + pad + textY;

        for (String s : drawLines) {
            ctx.drawText(tr, s, baseX, y, layout.textColor, false);
            y += lineH;
        }

        ctx.getMatrices().popMatrix();
    }

    private static String trimTextToWidth(TextRenderer tr, String raw, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (maxWidth <= 0 || tr.getWidth(text) <= maxWidth) {
            return text;
        }
        final String ellipsis = "...";
        int keep = text.length();
        while (keep > 0 && tr.getWidth(text.substring(0, keep) + ellipsis) > maxWidth) {
            keep--;
        }
        return keep <= 0 ? ellipsis : text.substring(0, keep) + ellipsis;
    }

    private static HudCanvasDataHandler.HudCanvasElement defaultHudCanvasLayout() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 12;
        e.y = 12;
        e.width = 240;
        e.height = 120;
        e.padding = 4;
        e.lineHeight = 9;
        e.fontScale = 0.85f;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        e.horizontalAlign = HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT;
        e.verticalAlign = HudCanvasDataHandler.HudCanvasElement.VerticalAlign.TOP;
        e.anchor = HudCanvasDataHandler.HudCanvasElement.Anchor.TOP_RIGHT;
        return e;
    }

    private static int resolvePanelX(HudCanvasDataHandler.HudCanvasElement canvas, int panelW, int screenW) {
        return switch (canvas.anchor) {
            case TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER -> Math.clamp((screenW - panelW) / 2 + canvas.x, 0, Math.max(0, screenW - panelW));
            case TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT -> Math.clamp(screenW - panelW - canvas.x, 0, Math.max(0, screenW - panelW));
            default -> Math.clamp(canvas.x, 0, Math.max(0, screenW - panelW));
        };
    }

    private static int resolvePanelY(HudCanvasDataHandler.HudCanvasElement canvas, int panelH, int screenH) {
        return switch (canvas.anchor) {
            case MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT -> Math.clamp((screenH - panelH) / 2 + canvas.y, 0, Math.max(0, screenH - panelH));
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> Math.clamp(screenH - panelH - canvas.y, 0, Math.max(0, screenH - panelH));
            default -> Math.clamp(canvas.y, 0, Math.max(0, screenH - panelH));
        };
    }

    private static int alignedTextX(HudCanvasDataHandler.HudCanvasElement canvas, TextRenderer tr, List<String> lines, int logicalInnerW) {
        if (canvas.horizontalAlign == HudCanvasDataHandler.HudCanvasElement.HorizontalAlign.LEFT) {
            return 0;
        }
        int maxLineWidth = 0;
        for (String line : lines) {
            maxLineWidth = Math.max(maxLineWidth, tr.getWidth(line));
        }
        return switch (canvas.horizontalAlign) {
            case CENTER -> Math.max(0, (logicalInnerW - maxLineWidth) / 2);
            case RIGHT -> Math.max(0, logicalInnerW - maxLineWidth);
            default -> 0;
        };
    }

    private static int alignedTextY(HudCanvasDataHandler.HudCanvasElement canvas, int logicalInnerH, int textBlockHeight) {
        return switch (canvas.verticalAlign) {
            case CENTER -> Math.max(0, (logicalInnerH - textBlockHeight) / 2);
            case BOTTOM -> Math.max(0, logicalInnerH - textBlockHeight);
            default -> 0;
        };
    }

    private static List<String> getInfoLines() {
        List<String> lines = new ArrayList<>();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return List.of();
        }

        Map<String, MacroDataHandler.MacroEntry> allMacros = MacroDataHandler.getAllMacros();
        boolean hasExplicitOverlayEntries = allMacros.values().stream().anyMatch(m -> m != null && m.showInOverlay);

        for (MacroDataHandler.MacroEntry me : allMacros.values()) {
            if (me == null) {
                continue;
            }

            // Backward-compat behavior: if no macro is explicitly opted in, show all configured macros.
            if (hasExplicitOverlayEntries && !me.showInOverlay) {
                continue;
            }

            int keyName = me.keyCode;
            String modifierKey = me.modifierKey;
            String displayName = (me.name == null || me.name.isBlank()) ? "Unnamed macro" : me.name;
            String displayKey = (modifierKey != null && !modifierKey.isEmpty() ? modifierKey + "+" : "") + (keyName >= 0 ? getKeyName(keyName) : "None");
            lines.add(displayName + " - [" + displayKey + "]");
        }

        return lines;
    }

    private static String getKeyName(int keyCode) {
        try {
            return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Visible: " + (ModConfig.showMacroKeybindOverlay ? "ON" : "OFF"));
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 0) {
            ModConfig.updateAndSave(() -> ModConfig.showMacroKeybindOverlay = !ModConfig.showMacroKeybindOverlay);
            return;
        }

        super.onSettingSelected(settingIndex);
    }
}
