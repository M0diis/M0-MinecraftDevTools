package me.m0dii.modules.chat;

import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class SecondaryChatOverlay {
    private SecondaryChatOverlay() {
    }

    private static int scrollOffset = 0;

    public static void register() {
        HudRenderCallback.EVENT.register(SecondaryChatOverlay::onHudRender);
    }

    public static void scroll(int amount) {
        scrollOffset += amount;
    }

    public static void resetScroll() {
        scrollOffset = 0;
    }

    private static void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        if (!ModConfig.secondaryChatEnabled || !ModConfig.secondaryChatShowOverlay) {
            return;
        }

        if (client.currentScreen != null && !ModConfig.secondaryChatShowWhileGuiOpen) {
            return;
        }

        TextRenderer tr = client.textRenderer;

        final float scale = (float) Math.max(0.1, ModConfig.secondaryChatScale);
        final int lineH = Math.max(1, ModConfig.secondaryChatLineHeight);
        final int pad = Math.max(0, ModConfig.secondaryChatPadding);

        int panelX = ModConfig.secondaryChatX;
        int panelY = ModConfig.secondaryChatY;
        int panelW = Math.max(50, ModConfig.secondaryChatWidth);
        int panelH = Math.max(30, ModConfig.secondaryChatHeight);

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        panelX = clamp(panelX, 0, Math.max(0, screenW - 5));
        panelY = clamp(panelY, 0, Math.max(0, screenH - 5));

        // Fade alpha
        int alpha = (ModConfig.secondaryChatBackgroundColor >> 24) & 0xFF;
        int textAlpha = (ModConfig.secondaryChatTextColor >> 24) & 0xFF;
        if (ModConfig.secondaryChatFadeEnabled) {
            long now = System.currentTimeMillis();
            long lastMsg = SecondaryChatManager.getLastMessageTime();
            long dt = now - lastMsg;
            if (dt > 0) {
                int fadeMs = Math.max(1000, ModConfig.secondaryChatFadeDurationMs);
                float fade = Math.clamp(1f - (float) dt / fadeMs, 0f, 1f);
                int minAlpha = Math.clamp(ModConfig.secondaryChatMinAlpha, 0, 255);
                alpha = Math.round(minAlpha + (alpha - minAlpha) * fade);
                textAlpha = Math.round(minAlpha + (textAlpha - minAlpha) * fade);
            }
        }
        int bg = (ModConfig.secondaryChatBackgroundColor & 0x00FFFFFF) | (alpha << 24);
        int textColor = (ModConfig.secondaryChatTextColor & 0x00FFFFFF) | (textAlpha << 24);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, bg);

        // Resize handle
        int handleAlpha = alpha; // Use fade alpha
        int handleColor = (0xFFFFFF) | (handleAlpha << 24);
        if (SecondaryChatInteraction.isDraggingOrResizing()) {
            // Highlight border
            int borderColor = (0x00FF00) | (handleAlpha << 24);
            ctx.drawBorder(panelX, panelY, panelW, panelH, borderColor);
        }

        int resizeX = panelX + panelW - 12;
        int resizeY = panelY + panelH - 12;
        ctx.fill(resizeX, resizeY, resizeX + 12, resizeY + 12, handleColor);

        List<Text> lines = SecondaryChatManager.snapshot();

        // Scaled text draw (convert to unscaled coords)
        ctx.getMatrices().push();
        Matrix4f m = new Matrix4f(ctx.getMatrices().peek().getPositionMatrix());
        ctx.getMatrices().peek().getPositionMatrix().set(m.scale(scale, scale, 1.0f));

        final int baseX = Math.round(panelX / scale) + pad;
        final int baseY = Math.round(panelY / scale) + pad;
        final int baseW = Math.round(panelW / scale) - pad * 2;
        final int baseH = Math.round(panelH / scale) - pad * 2;

        int y = baseY;

        // Prepare wrapped lines and scroll calculations outside the if-else so they are always in scope
        List<OrderedText> wrappedLines = new ArrayList<>();
        int maxLinesVisible = 1;
        int maxScroll = 0;
        if (!lines.isEmpty()) {
            for (Text t : lines) {
                List<OrderedText> split = tr.wrapLines(t, baseW);
                wrappedLines.addAll(split);
            }

            maxLinesVisible = Math.max(1, (baseH - lineH - 2) / lineH);

            // Clamp scroll offset
            maxScroll = Math.max(0, wrappedLines.size() - maxLinesVisible);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }

        // Show title
        String title = "Secondary Chat";
        ctx.drawText(tr, title, baseX, y, textColor, false);
        y += lineH + 2;

        if (lines.isEmpty()) {
            String hint = "Waiting for messages...";
            ctx.drawText(tr, hint, baseX, y, 0x888888, false);
        } else {
            int start = Math.max(0, wrappedLines.size() - maxLinesVisible - scrollOffset);
            int end = Math.min(wrappedLines.size(), start + maxLinesVisible);

            for (int i = start; i < end; i++) {
                if (y + lineH > baseY + baseH) {
                    break;
                }
                ctx.drawText(tr, wrappedLines.get(i), baseX, y, textColor, false);
                y += lineH;
            }
        }

        // Draw scroll indicators (in scaled coordinate space)
        if (wrappedLines.size() > maxLinesVisible && maxScroll > 0) {
            // Calculate scrollbar position in unscaled coordinates
            // Position it to avoid the resize handle (12x12 in bottom-right corner)
            int resizeHandleSize = Math.round(12 / scale); // Resize handle size in unscaled coords
            int scrollbarX = baseX + baseW + 2;  // 2 pixels right of content
            int scrollbarY = baseY + lineH + 2;  // Below title
            int scrollbarH = baseH - lineH - resizeHandleSize - 6;  // Stop before resize handle
            int scrollbarW = 3;  // Scrollbar width

            // Scroll bar background
            ctx.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarW, scrollbarY + scrollbarH, 0x40FFFFFF);

            // Scroll bar thumb
            float scrollPercent = (float) scrollOffset / maxScroll;
            int thumbSize = Math.max(8, scrollbarH * maxLinesVisible / wrappedLines.size());
            int thumbY = scrollbarY + (int) ((scrollbarH - thumbSize) * (1.0f - scrollPercent));

            ctx.fill(scrollbarX, thumbY, scrollbarX + scrollbarW, thumbY + thumbSize, 0xFFFFFFFF);

            int arrowGlyphSize = 8;
            if (scrollOffset > 0) {
                String upArrow = "▲";
                ctx.drawText(tr, upArrow, scrollbarX - 1, scrollbarY - arrowGlyphSize - 1, 0xFFFFFFFF, false);
            }
            if (scrollOffset < maxScroll) {
                String downArrow = "▼";
                ctx.drawText(tr, downArrow, scrollbarX - 1, scrollbarY + scrollbarH + 1, 0xFFFFFFFF, false);
            }
        }

        ctx.getMatrices().pop();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
