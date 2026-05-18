package me.m0dii.modules.macros.gui;

import me.m0dii.modules.Module;
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

        List<String> lines = getInfoLines();
        if (lines.isEmpty()) {
            return;
        }

        TextRenderer tr = client.textRenderer;

        float scale = (float) ModConfig.macroOverlayTextScale;
        if (Float.isNaN(scale) || scale <= 0f) {
            scale = 0.85f;
        }
        scale = Math.clamp(scale, 0.5f, 3.0f);

        final int lineH = Math.max(1, ModConfig.macroOverlayLineHeight);
        final int pad = Math.max(0, ModConfig.macroOverlayPadding);
        final int marginX = Math.max(0, ModConfig.macroOverlayMarginX);
        final int marginY = Math.max(0, ModConfig.macroOverlayMarginY);

        int maxW = tr.getWidth("Macro Keybinds");
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));

        // Base (unscaled) measurements.
        final int contentW = maxW;
        final int contentH = (1 + lines.size()) * lineH;
        final int panelW = contentW + pad * 2;
        final int panelH = contentH + pad * 2;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        // Compute anchor position in screen space (scaled pixels).
        final int panelWScaled = Math.round(panelW * scale);
        final int panelHScaled = Math.round(panelH * scale);

        int panelX;
        int panelY;

        ModConfig.OverlayAnchor anchor = ModConfig.macroOverlayAnchor;

        if (anchor == null) {
            anchor = ModConfig.OverlayAnchor.TOP_RIGHT;
        }

        switch (anchor) {
            case TOP_LEFT -> {
                panelX = marginX;
                panelY = marginY;
            }
            case TOP_RIGHT -> {
                panelX = screenW - marginX - panelWScaled;
                panelY = marginY;
            }
            case BOTTOM_LEFT -> {
                panelX = marginX;
                panelY = screenH - marginY - panelHScaled;
            }
            case BOTTOM_RIGHT -> {
                panelX = screenW - marginX - panelWScaled;
                panelY = screenH - marginY - panelHScaled;
            }
            default -> {
                panelX = screenW - marginX - panelWScaled;
                panelY = marginY;
            }
        }

        // Draw background in screen space (unscaled).
        ctx.fill(panelX, panelY, panelX + panelWScaled, panelY + panelHScaled, 0x88000000);

        // Draw text in scaled coordinates.
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

        // Convert screen-space to unscaled-space for text rendering.
        final int baseX = Math.round(panelX / scale) + pad;
        int y = Math.round(panelY / scale) + pad;

        ctx.drawText(tr, "Macro Keybinds", baseX, y, 0xFFFFFFFF, false);
        y += lineH;
        for (String s : lines) {
            ctx.drawText(tr, s, baseX, y, 0xFFC0C0C0, false);
            y += lineH;
        }

        ctx.getMatrices().popMatrix();
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
