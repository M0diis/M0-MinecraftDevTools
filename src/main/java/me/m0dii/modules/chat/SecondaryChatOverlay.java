package me.m0dii.modules.chat;

import me.m0dii.M0DevToolsClient;
import me.m0dii.gui.GuiSystem;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

public final class SecondaryChatOverlay {
    private SecondaryChatOverlay() {
    }

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(M0DevToolsClient.MOD_ID, "secondary_chat"),
                SecondaryChatOverlay::onHudRender
        );
    }

    public static void scroll(int amount) {
        SecondaryChatSettings.WindowConfig window = SecondaryChatManager.findWindow("main");
        if (window == null) {
            return;
        }
        SecondaryChatSettings.TabConfig tab = SecondaryChatManager.selectedTab(window);
        if (tab != null) {
            SecondaryChatManager.scroll(window.id, tab.id, amount);
        }
    }

    public static void resetScroll() {
        SecondaryChatManager.resetScroll();
    }

    public static boolean shouldSuppressVanillaChat() {
        MinecraftClient client = MinecraftClient.getInstance();
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || !settings.showOverlay || settings.renderMode != SecondaryChatSettings.RenderMode.REPLACE) {
            return false;
        }
        return client == null || client.currentScreen == null || settings.showWhileGuiOpen;
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

        TextRenderer textRenderer = client.textRenderer;
        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.frames()) {
            renderWindow(ctx, textRenderer, settings, frame, noTransparency, client.currentScreen != null);
        }
    }

    private static void renderWindow(DrawContext ctx,
                                     TextRenderer textRenderer,
                                     SecondaryChatSettings.Data settings,
                                     SecondaryChatWindowLayout.Frame frame,
                                     boolean noTransparency,
                                     boolean screenOpen) {
        int alpha = (frame.backgroundColor >> 24) & 0xFF;
        int textAlpha = (frame.textColor >> 24) & 0xFF;
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

        int bg = (frame.backgroundColor & 0x00FFFFFF) | (alpha << 24);
        int textColor = (frame.textColor & 0x00FFFFFF) | (textAlpha << 24);
        if (frame.drawBackground) {
            ctx.fill(frame.x, frame.y, frame.x + frame.width, frame.y + frame.height, bg);
        }
        if (frame.drawBorder) {
            GuiSystem.drawOutline(ctx, frame.x, frame.y, frame.x + frame.width, frame.y + frame.height, frame.borderColor);
        }

        if (screenOpen && SecondaryChatInteraction.isDraggingOrResizing(frame.window.id)) {
            GuiSystem.drawOutline(ctx, frame.x, frame.y, frame.x + frame.width, frame.y + frame.height,
                    (0x00FF66) | (Math.max(alpha, 120) << 24));
        }

        if (frame.window.showTabs) {
            renderTabs(ctx, textRenderer, frame, textColor);
        }

        renderMessages(ctx, textRenderer, frame, textColor);

        if (screenOpen && frame.window.showTabs) {
            renderSettingsButton(ctx, frame, alpha);
            if (SecondaryChatInteraction.isMenuOpen(frame.window.id)) {
                renderSettingsMenu(ctx, textRenderer, frame, textColor);
            }
        }

        if (screenOpen) {
            int handleColor = 0x00FFFFFF | (Math.max(alpha, 120) << 24);
            int x = frame.x + frame.width - SecondaryChatWindowLayout.RESIZE_HANDLE;
            int y = frame.y + frame.height - SecondaryChatWindowLayout.RESIZE_HANDLE;
            ctx.fill(x, y, x + SecondaryChatWindowLayout.RESIZE_HANDLE, y + SecondaryChatWindowLayout.RESIZE_HANDLE, handleColor);
        }

        if (screenOpen) {
            renderMessageHover(ctx, textRenderer, frame);
        }
    }

    private static void renderTabs(DrawContext ctx,
                                   TextRenderer textRenderer,
                                   SecondaryChatWindowLayout.Frame frame,
                                   int textColor) {
        SecondaryChatSettings.TabConfig selected = SecondaryChatManager.selectedTab(frame.window);
        int tabX = frame.x + 2;
        int tabY = frame.y + 2;
        int tabH = SecondaryChatWindowLayout.HEADER_HEIGHT - 3;
        int maxX = frame.x + frame.width - SecondaryChatWindowLayout.SETTINGS_BUTTON_SIZE - 4;
        for (SecondaryChatSettings.TabConfig tab : frame.window.tabs) {
            int remaining = maxX - tabX;
            if (remaining < 28) {
                break;
            }
            int tabW = Math.min(SecondaryChatWindowLayout.tabWidth(frame.window.id, tab, textRenderer), remaining);

            boolean active = selected != null && selected.id.equals(tab.id);
            int fill = active ? 0xAA30343B : 0x6616181C;
            int outline = active ? 0xFFE0E0E0 : 0x55FFFFFF;
            ctx.fill(tabX, tabY, tabX + tabW, tabY + tabH, fill);
            GuiSystem.drawOutline(ctx, tabX, tabY, tabX + tabW, tabY + tabH, outline);

            String label = trimToWidth(textRenderer,
                    SecondaryChatWindowLayout.tabLabel(frame.window.id, tab),
                    Math.max(8, tabW - 10));
            ctx.drawText(textRenderer, label, tabX + 5, tabY + 3, textColor, false);
            tabX += tabW + 2;
        }
    }

    private static void renderSettingsButton(DrawContext ctx,
                                             SecondaryChatWindowLayout.Frame frame,
                                             int alpha) {
        int x = SecondaryChatWindowLayout.settingsButtonX(frame);
        int y = SecondaryChatWindowLayout.settingsButtonY(frame);
        int size = SecondaryChatWindowLayout.SETTINGS_BUTTON_SIZE;
        boolean open = SecondaryChatInteraction.isMenuOpen(frame.window.id);
        int baseFill = open ? 0x00343A44 : 0x0016181C;
        int fill = baseFill | (Math.max(alpha, 90) << 24);
        ctx.fill(x, y, x + size, y + size, fill);

        int line = 0xDDFFFFFF;
        ctx.fill(x + 3, y + 2, x + size - 3, y + 3, line);
        ctx.fill(x + 3, y + 5, x + size - 3, y + 6, line);
        ctx.fill(x + 3, y + 8, x + size - 3, y + 9, line);
    }

    private static void renderSettingsMenu(DrawContext ctx,
                                           TextRenderer textRenderer,
                                           SecondaryChatWindowLayout.Frame frame,
                                           int textColor) {
        int x = SecondaryChatWindowLayout.menuX(frame);
        int y = SecondaryChatWindowLayout.menuY(frame);
        int w = SecondaryChatWindowLayout.MENU_WIDTH;
        int h = SecondaryChatWindowLayout.MENU_ROW_HEIGHT * SecondaryChatWindowLayout.MENU_ROWS;
        ctx.fill(x, y, x + w, y + h, 0xEE14161A);
        GuiSystem.drawOutline(ctx, x, y, x + w, y + h, 0xDDFFFFFF);

        for (int row = 0; row < SecondaryChatWindowLayout.MENU_ROWS; row++) {
            int rowY = y + row * SecondaryChatWindowLayout.MENU_ROW_HEIGHT;
            if (row > 0) {
                ctx.fill(x + 1, rowY, x + w - 1, rowY + 1, 0x33FFFFFF);
            }
            boolean enabled = SecondaryChatInteraction.menuItemEnabled(frame, row);
            int color = enabled ? textColor : 0x77888888;
            String label = trimToWidth(textRenderer, SecondaryChatInteraction.menuLabel(row), w - 12);
            ctx.drawText(textRenderer, label, x + 6, rowY + 4, color, false);
        }
    }

    private static void renderMessages(DrawContext ctx,
                                       TextRenderer textRenderer,
                                       SecondaryChatWindowLayout.Frame frame,
                                       int textColor) {
        SecondaryChatSettings.TabConfig selected = SecondaryChatManager.selectedTab(frame.window);
        if (selected == null) {
            return;
        }

        SecondaryChatTextLayout.Layout layout = SecondaryChatTextLayout.layout(frame, textRenderer);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(frame.scale, frame.scale);
        ctx.enableScissor(layout.x(), layout.y(), layout.x() + layout.scaledWidth(), layout.contentBottom());

        if (layout.totalLines() == 0) {
            String hint = frame.window.title == null || frame.window.title.isBlank() ? "Waiting for messages..." : frame.window.title;
            ctx.drawText(textRenderer, hint, layout.x(), layout.y(), 0x88888888, false);
        } else {
            for (SecondaryChatTextLayout.VisibleLine line : layout.visibleLines()) {
                if (SecondaryChatInteraction.isSelected(frame.window.id, selected.id, line.message())) {
                    int selectionRight = Math.min(layout.x() + layout.scaledWidth(), line.x() + Math.max(1, line.width()));
                    ctx.fill(line.x() - 1, line.y() - 1, selectionRight + 1, line.y() + layout.lineHeight(), 0x553B82F6);
                }
                ctx.drawText(textRenderer, line.text(), line.x(), line.y(), textColor, false);
            }
        }

        ctx.disableScissor();

        if (layout.totalLines() > layout.maxVisible() && layout.maxScroll() > 0) {
            int scrollbarX = Math.round((frame.x + frame.width - 5) / frame.scale);
            int scrollbarY = Math.round((frame.contentY()) / frame.scale);
            int scrollbarH = Math.max(12, Math.round((frame.contentHeight() - 10) / frame.scale));
            int thumbSize = Math.max(8, scrollbarH * layout.maxVisible() / layout.totalLines());
            float scrollPercent = (float) (layout.scroll() / layout.maxScroll());
            int thumbY = scrollbarY + (int) ((scrollbarH - thumbSize) * (1.0f - scrollPercent));
            ctx.fill(scrollbarX, scrollbarY, scrollbarX + 2, scrollbarY + scrollbarH, 0x40FFFFFF);
            ctx.fill(scrollbarX, thumbY, scrollbarX + 2, thumbY + thumbSize, 0xCCFFFFFF);
        }

        ctx.getMatrices().popMatrix();
    }

    private static void renderMessageHover(DrawContext ctx,
                                           TextRenderer textRenderer,
                                           SecondaryChatWindowLayout.Frame frame) {
        SecondaryChatTextLayout.Hit hit = SecondaryChatInteraction.hoveredMessageHit(frame, textRenderer);
        if (hit == null || hit.style() == null) {
            return;
        }

        Style style = hit.style();
        if (style.getClickEvent() != null) {
            ctx.setCursor(StandardCursors.POINTING_HAND);
        }
        if (style.getHoverEvent() != null) {
            ctx.drawHoverEvent(textRenderer, style, SecondaryChatInteraction.lastMouseX(), SecondaryChatInteraction.lastMouseY());
        }
    }

    private static String trimToWidth(TextRenderer textRenderer, String raw, int maxWidth) {
        String text = raw == null ? "" : raw;
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        while (text.length() > 1 && textRenderer.getWidth(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    static HudCanvasDataHandler.HudCanvasElement defaultCanvasElement() {
        HudCanvasDataHandler.HudCanvasElement e = new HudCanvasDataHandler.HudCanvasElement();
        e.x = 8;
        e.y = 40;
        e.width = 320;
        e.height = 160;
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
