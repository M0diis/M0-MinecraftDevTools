package me.m0dii.modules.macros.hud;

import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.List;

public class PendingMacrosHudModule extends Module {

    public static final PendingMacrosHudModule INSTANCE = new PendingMacrosHudModule();

    private PendingMacrosHudModule() {
        super("pending_macros_hud", "Pending Macros HUD", true);
    }

    @Override
    public void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "pending_macros_hud"),
                this::onHudRender
        );
    }

    private void onHudRender(GuiGraphicsExtractor ctx, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || isClientNull()) {
            return;
        }

        List<String> lines = CommandMacros.getPendingDisplayLines();
        if (lines.isEmpty()) {
            return;
        }

        Font tr = client.font;

        final float scale = Math.clamp((float) ModConfig.macroOverlayTextScale, 0.5f, 3.0f);
        final int lineH = Math.max(1, ModConfig.macroOverlayLineHeight);
        final int pad = Math.max(0, ModConfig.macroOverlayPadding);
        final int marginX = Math.max(0, ModConfig.macroOverlayMarginX);
        final int marginY = Math.max(0, ModConfig.macroOverlayMarginY);

        int maxW = tr.width("Macros pending:");
        for (String s : lines) maxW = Math.max(maxW, tr.width(s));

        final int contentW = maxW;
        final int contentH = (1 + lines.size()) * lineH;
        final int panelW = contentW + pad * 2;
        final int panelH = contentH + pad * 2;

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        final int panelWScaled = Math.round(panelW * scale);
        final int panelHScaled = Math.round(panelH * scale);

        int panelX;
        int panelY;
        ModConfig.OverlayAnchor anchor = ModConfig.pendingMacrosAnchor;
        if (anchor == null) {
            anchor = ModConfig.OverlayAnchor.TOP_LEFT;
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
                panelX = marginX;
                panelY = marginY;
            }
        }

        ctx.fill(panelX, panelY, panelX + panelWScaled, panelY + panelHScaled, 0x88000000);

        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);

        final int baseX = Math.round(panelX / scale) + pad;
        int y = Math.round(panelY / scale) + pad;

        ctx.text(tr, "Macros pending:", baseX, y, 0xFFFFFFFF, false);
        y += lineH;
        for (String s : lines) {
            ctx.text(tr, s, baseX, y, 0xFFC0C0C0, false);
            y += lineH;
        }

        ctx.pose().popMatrix();
    }
}
