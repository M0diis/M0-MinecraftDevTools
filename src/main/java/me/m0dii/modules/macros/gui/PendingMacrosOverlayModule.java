package me.m0dii.modules.macros.gui;

import me.m0dii.modules.Module;
import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;

import java.util.List;

public class PendingMacrosOverlayModule extends Module {

    public static final PendingMacrosOverlayModule INSTANCE = new PendingMacrosOverlayModule();

    private PendingMacrosOverlayModule() {
        super("pending_macros_hud", "Pending Macros HUD", true);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled() || isClientNull()) {
            return;
        }

        List<String> lines = CommandMacros.getPendingDisplayLines();
        if (lines.isEmpty()) {
            return;
        }

        TextRenderer tr = client.textRenderer;

        final float scale = Math.clamp((float) ModConfig.macroOverlayTextScale, 0.5f, 3.0f);
        final int lineH = Math.max(1, ModConfig.macroOverlayLineHeight);
        final int pad = Math.max(0, ModConfig.macroOverlayPadding);
        final int marginX = Math.max(0, ModConfig.macroOverlayMarginX);
        final int marginY = Math.max(0, ModConfig.macroOverlayMarginY);

        int maxW = tr.getWidth("Macros pending:");
        for (String s : lines) maxW = Math.max(maxW, tr.getWidth(s));

        final int contentW = maxW;
        final int contentH = (1 + lines.size()) * lineH;
        final int panelW = contentW + pad * 2;
        final int panelH = contentH + pad * 2;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

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

        ctx.getMatrices().push();
        Matrix4f m = new Matrix4f(ctx.getMatrices().peek().getPositionMatrix());
        ctx.getMatrices().peek().getPositionMatrix().set(m.scale(scale, scale, 1.0f));

        final int baseX = Math.round(panelX / scale) + pad;
        int y = Math.round(panelY / scale) + pad;

        ctx.drawText(tr, "Macros pending:", baseX, y, 0xFFFFFF, false);
        y += lineH;
        for (String s : lines) {
            ctx.drawText(tr, s, baseX, y, 0xC0C0C0, false);
            y += lineH;
        }

        ctx.getMatrices().pop();
    }
}
