package me.m0dii.modules.chat;

import me.m0dii.M0DevToolsClient;
import me.m0dii.gui.GuiSystem;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class SecondaryChatOverlay {
    private SecondaryChatOverlay() {
    }

    private static int scrollOffset = 0;

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(M0DevToolsClient.MOD_ID, "secondary_chat"),
                SecondaryChatOverlay::onHudRender
        );
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

        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || !settings.showOverlay) {
            return;
        }

        if (client.currentScreen != null && !settings.showWhileGuiOpen) {
            return;
        }

        boolean noTransparency = client.currentScreen instanceof ChatScreen
                && settings.noTransparencyWhenChatOpen;

        TextRenderer tr = client.textRenderer;
        HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                SecondaryChatOverlay::defaultCanvasElement
        );
        if (!canvas.visible) {
            return;
        }

        final float scale = Math.max(0.1f, canvas.fontScale);
        final int lineH = Math.max(1, canvas.lineHeight);
        final int pad = Math.max(0, canvas.padding);

        int panelX = canvas.x;
        int panelY = canvas.y;
        int panelW = Math.max(50, canvas.width);
        int panelH = Math.max(30, canvas.height);

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        panelX = Math.clamp(panelX, 0, Math.max(0, screenW - 5));
        panelY = Math.clamp(panelY, 0, Math.max(0, screenH - 5));

        int alpha = (canvas.backgroundColor >> 24) & 0xFF;
        int textAlpha = (canvas.textColor >> 24) & 0xFF;
        if (settings.fadeEnabled && !noTransparency) {
            long now = System.currentTimeMillis();
            long lastMsg = SecondaryChatManager.getLastAlphaReset();
            long dt = now - lastMsg;
            if (dt > 0) {
                int fadeMs = Math.max(1000, settings.fadeDurationMs);
                float fade = Math.clamp(1f - (float) dt / fadeMs, 0f, 1f);
                int minAlpha = Math.clamp(settings.minAlpha, 0, 255);
                alpha = Math.round(minAlpha + (alpha - minAlpha) * fade);
                textAlpha = Math.round(minAlpha + (textAlpha - minAlpha) * fade);
            }
        }
        int bg = (canvas.backgroundColor & 0x00FFFFFF) | (alpha << 24);
        int textColor = (canvas.textColor & 0x00FFFFFF) | (textAlpha << 24);
        if (canvas.drawBackground) {
            ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, bg);
        }
        if (canvas.drawBorder) {
            GuiSystem.drawOutline(ctx, panelX, panelY, panelX + panelW, panelY + panelH, canvas.borderColor);
        }

        // Resize handle
        int handleAlpha = alpha; // Use fade alpha
        int handleColor = (0xFFFFFF) | (handleAlpha << 24);
        if (SecondaryChatInteraction.isDraggingOrResizing()) {
            // Highlight border
            int borderColor = (0x00FF00) | (handleAlpha << 24);
            GuiSystem.drawOutline(ctx, panelX, panelY, panelX + panelW, panelY + panelH, borderColor);
        }

        int resizeX = panelX + panelW - 6;
        int resizeY = panelY + panelH - 6;
        ctx.fill(resizeX, resizeY, resizeX + 6, resizeY + 6, handleColor);

        List<Text> lines = SecondaryChatManager.snapshot();

        // Scaled text draw (convert to unscaled coords)
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);

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
            scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
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

        ctx.getMatrices().popMatrix();
    }

    static HudCanvasDataHandler.HudCanvasElement defaultCanvasElement() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 40;
        e.width = 260;
        e.height = 120;
        e.fontScale = 0.85f;
        e.lineHeight = 9;
        e.padding = 4;
        e.backgroundColor = 0x88000000;
        e.textColor = 0xFFE0E0E0;
        e.borderColor = 0xFFFFFFFF;
        e.drawBackground = true;
        e.drawBorder = false;
        e.visible = true;
        return e;
    }

}
