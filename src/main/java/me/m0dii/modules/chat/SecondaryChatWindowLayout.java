package me.m0dii.modules.chat;

import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SecondaryChatWindowLayout {
    static final int HEADER_HEIGHT = 16;
    static final int RESIZE_HANDLE = 7;
    static final int SETTINGS_BUTTON_SIZE = 12;
    static final int MENU_WIDTH = 104;
    static final int MENU_ROW_HEIGHT = 16;
    static final int MENU_ROWS = 5;
    private static final int TAB_PAD_X = 6;
    private static final int TAB_GAP = 2;

    private SecondaryChatWindowLayout() {
    }

    static final class Frame {
        final SecondaryChatSettings.WindowConfig window;
        final boolean hudCanvasBacked;
        int x;
        int y;
        int width;
        int height;
        int padding;
        int lineHeight;
        float scale;
        int zIndex;
        int backgroundColor;
        int textColor;
        int borderColor;
        boolean drawBackground;
        boolean drawBorder;
        boolean visible;

        private Frame(SecondaryChatSettings.WindowConfig window, boolean hudCanvasBacked) {
            this.window = window;
            this.hudCanvasBacked = hudCanvasBacked;
        }

        int headerHeight() {
            return window.showTabs ? HEADER_HEIGHT : 0;
        }

        int contentX() {
            return x + padding;
        }

        int contentY() {
            return y + headerHeight() + padding;
        }

        int contentWidth() {
            return Math.max(1, width - padding * 2);
        }

        int contentHeight() {
            return Math.max(1, height - headerHeight() - padding * 2);
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        boolean containsResize(double mouseX, double mouseY) {
            return mouseX >= x + width - RESIZE_HANDLE && mouseX <= x + width
                    && mouseY >= y + height - RESIZE_HANDLE && mouseY <= y + height;
        }
    }

    static final class TabHit {
        final SecondaryChatSettings.WindowConfig window;
        final SecondaryChatSettings.TabConfig tab;

        private TabHit(SecondaryChatSettings.WindowConfig window, SecondaryChatSettings.TabConfig tab) {
            this.window = window;
            this.tab = tab;
        }
    }

    static List<Frame> frames() {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        List<Frame> frames = new ArrayList<>();
        for (SecondaryChatSettings.WindowConfig window : settings.windows) {
            Frame frame = frame(window);
            if (frame.visible) {
                frames.add(frame);
            }
        }
        frames.sort(Comparator.comparingInt(frame -> frame.zIndex));
        return frames;
    }

    static List<Frame> framesTopFirst() {
        List<Frame> frames = frames();
        frames.sort(Comparator.comparingInt((Frame frame) -> frame.zIndex).reversed());
        return frames;
    }

    static Frame frame(SecondaryChatSettings.WindowConfig window) {
        boolean useCanvas = window.useHudCanvas || "main".equals(window.id);
        Frame frame = new Frame(window, useCanvas);
        if (useCanvas) {
            HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                    HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                    SecondaryChatOverlay::defaultCanvasElement
            );
            frame.x = canvas.x;
            frame.y = canvas.y;
            frame.width = Math.max(120, canvas.width);
            frame.height = Math.max(60, canvas.height);
            frame.padding = Math.max(0, canvas.padding);
            frame.lineHeight = Math.max(6, canvas.lineHeight);
            frame.scale = Math.max(0.25f, canvas.fontScale);
            frame.zIndex = canvas.zIndex;
            frame.backgroundColor = canvas.backgroundColor;
            frame.textColor = canvas.textColor;
            frame.borderColor = canvas.borderColor;
            frame.drawBackground = canvas.drawBackground;
            frame.drawBorder = canvas.drawBorder;
            frame.visible = canvas.visible && window.visible;
        } else {
            frame.x = window.x;
            frame.y = window.y;
            frame.width = Math.max(120, window.width);
            frame.height = Math.max(60, window.height);
            frame.padding = Math.max(0, window.padding);
            frame.lineHeight = Math.max(6, window.lineHeight);
            frame.scale = Math.max(0.25f, window.fontScale);
            frame.zIndex = window.zIndex;
            frame.backgroundColor = window.backgroundColor;
            frame.textColor = window.textColor;
            frame.borderColor = window.borderColor;
            frame.drawBackground = window.drawBackground;
            frame.drawBorder = window.drawBorder;
            frame.visible = window.visible;
        }
        clampToScreen(frame);
        return frame;
    }

    static TabHit tabAt(Frame frame, double mouseX, double mouseY, TextRenderer textRenderer) {
        if (!frame.window.showTabs || mouseY < frame.y || mouseY > frame.y + HEADER_HEIGHT) {
            return null;
        }

        int x = frame.x + 2;
        int maxX = frame.x + frame.width - SETTINGS_BUTTON_SIZE - 4;
        for (SecondaryChatSettings.TabConfig tab : frame.window.tabs) {
            int remaining = maxX - x;
            if (remaining < 28) {
                break;
            }
            int width = Math.min(tabWidth(frame.window.id, tab, textRenderer), remaining);
            if (mouseX >= x && mouseX <= x + width) {
                return new TabHit(frame.window, tab);
            }
            x += width + TAB_GAP;
        }
        return null;
    }

    static int tabWidth(String windowId, SecondaryChatSettings.TabConfig tab, TextRenderer textRenderer) {
        return Math.max(28, textRenderer.getWidth(tabLabel(windowId, tab)) + TAB_PAD_X * 2);
    }

    static String tabLabel(String windowId, SecondaryChatSettings.TabConfig tab) {
        int unread = SecondaryChatManager.unreadCount(windowId, tab.id);
        return unread > 0 ? tab.name + " (+" + Math.min(unread, 99) + ")" : tab.name;
    }

    static int settingsButtonX(Frame frame) {
        return frame.x + frame.width - SETTINGS_BUTTON_SIZE - 2;
    }

    static int settingsButtonY(Frame frame) {
        return frame.y + 2;
    }

    static boolean containsSettingsButton(Frame frame, double mouseX, double mouseY) {
        int x = settingsButtonX(frame);
        int y = settingsButtonY(frame);
        return frame.window.showTabs
                && mouseX >= x
                && mouseX <= x + SETTINGS_BUTTON_SIZE
                && mouseY >= y
                && mouseY <= y + SETTINGS_BUTTON_SIZE;
    }

    static int menuX(Frame frame) {
        return Math.max(frame.x + 2, frame.x + frame.width - MENU_WIDTH - 2);
    }

    static int menuY(Frame frame) {
        return frame.y + HEADER_HEIGHT + 2;
    }

    static boolean containsMenu(Frame frame, double mouseX, double mouseY) {
        int x = menuX(frame);
        int y = menuY(frame);
        return mouseX >= x
                && mouseX <= x + MENU_WIDTH
                && mouseY >= y
                && mouseY <= y + MENU_ROW_HEIGHT * MENU_ROWS;
    }

    static void writeFrameBounds(Frame frame, int x, int y, int width, int height) {
        if (frame.hudCanvasBacked) {
            HudCanvasDataHandler.HudCanvasElement canvas = HudCanvasDataHandler.getMutableElement(
                    HudCanvasDataHandler.ELEMENT_SECONDARY_CHAT,
                    SecondaryChatOverlay::defaultCanvasElement
            );
            canvas.x = x;
            canvas.y = y;
            canvas.width = Math.max(120, width);
            canvas.height = Math.max(60, height);
            return;
        }

        frame.window.x = x;
        frame.window.y = y;
        frame.window.width = Math.max(120, width);
        frame.window.height = Math.max(60, height);
    }

    private static void clampToScreen(Frame frame) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        frame.x = Math.clamp(frame.x, 0, Math.max(0, screenW - 5));
        frame.y = Math.clamp(frame.y, 0, Math.max(0, screenH - 5));
    }
}
