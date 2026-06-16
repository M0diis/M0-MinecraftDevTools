package me.m0dii.modules.chat;

import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;
import java.util.Locale;

public final class SecondaryChatInteraction {
    private SecondaryChatInteraction() {
    }

    private enum DragMode {
        NONE,
        MOVE,
        RESIZE
    }

    private static DragMode dragMode = DragMode.NONE;
    private static String activeWindowId = "";
    private static boolean activeUsesHudCanvas = false;
    private static int dragStartX;
    private static int dragStartY;
    private static int dragStartWindowX;
    private static int dragStartWindowY;
    private static int dragStartWindowWidth;
    private static int dragStartWindowHeight;
    private static String openMenuWindowId = "";

    private static boolean hudCanvasDirty = false;
    private static boolean settingsDirty = false;
    private static Screen lastScreen = null;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.beforeMouseClick(screen).register(SecondaryChatInteraction::handleMouseClick);
            ScreenMouseEvents.beforeMouseRelease(screen).register(SecondaryChatInteraction::handleMouseRelease);
            ScreenMouseEvents.beforeMouseScroll(screen).register(SecondaryChatInteraction::handleMouseScroll);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != lastScreen) {
                saveIfDirty();

                if (client.currentScreen == null) {
                    stopDragging();
                }

                lastScreen = client.currentScreen;
            }
        });
    }

    private static void handleMouseClick(Screen screen, Click click) {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || !settings.showOverlay) {
            return;
        }

        if (click.button() != 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        double mouseX = click.x();
        double mouseY = click.y();

        SecondaryChatWindowLayout.Frame menuFrame = openMenuFrame();
        if (menuFrame != null && SecondaryChatWindowLayout.containsMenu(menuFrame, mouseX, mouseY)) {
            handleMenuClick(menuFrame, mouseX, mouseY);
            return;
        }

        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (!frame.contains(mouseX, mouseY)) {
                continue;
            }

            if (SecondaryChatWindowLayout.containsSettingsButton(frame, mouseX, mouseY)) {
                toggleMenu(frame.window.id);
                return;
            }

            openMenuWindowId = "";

            SecondaryChatWindowLayout.TabHit tabHit = SecondaryChatWindowLayout.tabAt(frame, mouseX, mouseY, client.textRenderer);
            if (tabHit != null) {
                SecondaryChatManager.selectTab(tabHit.window.id, tabHit.tab.id);
                return;
            }

            activeWindowId = frame.window.id;
            activeUsesHudCanvas = frame.hudCanvasBacked;
            dragStartX = (int) mouseX;
            dragStartY = (int) mouseY;
            dragStartWindowX = frame.x;
            dragStartWindowY = frame.y;
            dragStartWindowWidth = frame.width;
            dragStartWindowHeight = frame.height;

            dragMode = frame.containsResize(mouseX, mouseY) ? DragMode.RESIZE : DragMode.MOVE;
            return;
        }

        openMenuWindowId = "";
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (dragMode == DragMode.NONE) {
            maybeResetTransparency(mouseX, mouseY);
            return;
        }

        SecondaryChatSettings.WindowConfig window = SecondaryChatManager.findWindow(activeWindowId);
        if (window == null) {
            stopDragging();
            return;
        }

        SecondaryChatWindowLayout.Frame frame = SecondaryChatWindowLayout.frame(window);
        int dx = (int) mouseX - dragStartX;
        int dy = (int) mouseY - dragStartY;

        int newX = dragStartWindowX;
        int newY = dragStartWindowY;
        int newWidth = dragStartWindowWidth;
        int newHeight = dragStartWindowHeight;
        if (dragMode == DragMode.MOVE) {
            newX += dx;
            newY += dy;
        } else if (dragMode == DragMode.RESIZE) {
            newWidth = Math.max(120, dragStartWindowWidth + dx);
            newHeight = Math.max(60, dragStartWindowHeight + dy);
        }

        SecondaryChatWindowLayout.writeFrameBounds(frame, newX, newY, newWidth, newHeight);
        if (activeUsesHudCanvas) {
            hudCanvasDirty = true;
        } else {
            settingsDirty = true;
        }
    }

    private static void handleMouseRelease(Screen screen, Click click) {
        if (click.button() != 0 || dragMode == DragMode.NONE) {
            return;
        }

        stopDragging();
        saveIfDirty();
    }

    private static void handleMouseScroll(Screen screen,
                                          double mouseX,
                                          double mouseY,
                                          double horizontalAmount,
                                          double verticalAmount) {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || !settings.showOverlay) {
            return;
        }

        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (!frame.contains(mouseX, mouseY)) {
                continue;
            }

            SecondaryChatSettings.TabConfig tab = SecondaryChatManager.selectedTab(frame.window);
            if (tab == null) {
                return;
            }
            int scrollAmount = (int) Math.signum(verticalAmount);
            SecondaryChatManager.scroll(frame.window.id, tab.id, scrollAmount);
            return;
        }
    }

    public static boolean isDraggingOrResizing() {
        return dragMode != DragMode.NONE;
    }

    public static boolean isDraggingOrResizing(String windowId) {
        return dragMode != DragMode.NONE && activeWindowId.equals(windowId);
    }

    static boolean isMenuOpen(String windowId) {
        return openMenuWindowId.equals(windowId);
    }

    static String menuLabel(int row) {
        return switch (row) {
            case 0 -> "New Tab";
            case 1 -> "Delete Tab";
            case 2 -> "Clear Tab";
            case 3 -> "Clear Window";
            case 4 -> "Editor";
            default -> "";
        };
    }

    static boolean menuItemEnabled(SecondaryChatWindowLayout.Frame frame, int row) {
        if (frame == null || frame.window == null) {
            return false;
        }
        SecondaryChatSettings.TabConfig selected = SecondaryChatManager.selectedTab(frame.window);
        return switch (row) {
            case 0 -> true;
            case 1 -> frame.window.tabs.size() > 1 && selected != null;
            case 2 -> selected != null;
            case 3 -> true;
            case 4 -> true;
            default -> false;
        };
    }

    private static void maybeResetTransparency(double mouseX, double mouseY) {
        if (!SecondaryChatSettings.get().resetTransparencyWhenHovered) {
            return;
        }
        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (frame.contains(mouseX, mouseY)) {
                SecondaryChatManager.setLastAlphaReset(System.currentTimeMillis());
                return;
            }
        }
    }

    private static void stopDragging() {
        dragMode = DragMode.NONE;
        activeWindowId = "";
        activeUsesHudCanvas = false;
    }

    private static void toggleMenu(String windowId) {
        openMenuWindowId = openMenuWindowId.equals(windowId) ? "" : windowId;
    }

    private static SecondaryChatWindowLayout.Frame openMenuFrame() {
        if (openMenuWindowId.isEmpty()) {
            return null;
        }
        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (frame.window.id.equals(openMenuWindowId)) {
                return frame;
            }
        }
        openMenuWindowId = "";
        return null;
    }

    private static void handleMenuClick(SecondaryChatWindowLayout.Frame frame, double mouseX, double mouseY) {
        int row = (int) ((mouseY - SecondaryChatWindowLayout.menuY(frame)) / SecondaryChatWindowLayout.MENU_ROW_HEIGHT);
        if (!menuItemEnabled(frame, row)) {
            return;
        }

        switch (row) {
            case 0 -> addTab(frame.window.id);
            case 1 -> removeSelectedTab(frame.window.id);
            case 2 -> clearSelectedTab(frame.window);
            case 3 -> SecondaryChatManager.clearWindow(frame.window.id);
            case 4 -> openEditor();
            default -> {
                return;
            }
        }

        if (row != 4) {
            openMenuWindowId = "";
        }
    }

    private static void addTab(String windowId) {
        final String[] addedTabId = {null};
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindow(windowId, SecondaryChatSettings.get().windows);
            if (window == null) {
                return;
            }
            int next = window.tabs.size() + 1;
            SecondaryChatSettings.TabConfig tab = new SecondaryChatSettings.TabConfig();
            tab.id = uniqueTabId("tab_" + next, window.tabs);
            tab.name = "Tab " + next;
            tab.catchAll = false;
            window.tabs.add(tab);
            window.selectedTabId = tab.id;
            addedTabId[0] = tab.id;
        });
        if (addedTabId[0] != null) {
            SecondaryChatManager.clearUnread(windowId, addedTabId[0]);
            SecondaryChatManager.resetScroll(windowId, addedTabId[0]);
        }
    }

    private static void removeSelectedTab(String windowId) {
        SecondaryChatSettings.WindowConfig currentWindow = SecondaryChatManager.findWindow(windowId);
        SecondaryChatSettings.TabConfig currentTab = currentWindow == null ? null : SecondaryChatManager.selectedTab(currentWindow);
        if (currentWindow == null || currentTab == null || currentWindow.tabs.size() <= 1) {
            return;
        }

        String removedTabId = currentTab.id;
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindow(windowId, SecondaryChatSettings.get().windows);
            if (window == null || window.tabs.size() <= 1) {
                return;
            }
            window.tabs.removeIf(tab -> tab.id.equals(removedTabId));
            if (window.tabs.isEmpty()) {
                SecondaryChatSettings.TabConfig tab = new SecondaryChatSettings.TabConfig();
                tab.id = "all";
                tab.name = "All";
                tab.catchAll = true;
                window.tabs.add(tab);
            }
            window.selectedTabId = window.tabs.getFirst().id;
        });
        SecondaryChatManager.clear(windowId, removedTabId);
    }

    private static void clearSelectedTab(SecondaryChatSettings.WindowConfig window) {
        SecondaryChatSettings.TabConfig tab = SecondaryChatManager.selectedTab(window);
        if (tab != null) {
            SecondaryChatManager.clear(window.id, tab.id);
        }
    }

    private static void openEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            openMenuWindowId = "";
            client.setScreen(new SecondaryChatConfigScreen(client.currentScreen));
        }
    }

    private static SecondaryChatSettings.WindowConfig findWindow(String windowId,
                                                                 List<SecondaryChatSettings.WindowConfig> windows) {
        if (windows == null) {
            return null;
        }
        for (SecondaryChatSettings.WindowConfig window : windows) {
            if (window != null && window.id.equals(windowId)) {
                return window;
            }
        }
        return null;
    }

    private static String uniqueTabId(String base, List<SecondaryChatSettings.TabConfig> tabs) {
        String root = sanitizeId(base);
        String candidate = root;
        int suffix = 2;
        while (containsTabId(tabs, candidate)) {
            candidate = root + "_" + suffix++;
        }
        return candidate;
    }

    private static boolean containsTabId(List<SecondaryChatSettings.TabConfig> tabs, String id) {
        if (tabs == null) {
            return false;
        }
        for (SecondaryChatSettings.TabConfig tab : tabs) {
            if (tab != null && tab.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitizeId(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return value.isEmpty() ? "tab" : value;
    }

    private static void saveIfDirty() {
        if (hudCanvasDirty) {
            HudCanvasDataHandler.save();
            hudCanvasDirty = false;
        }
        if (settingsDirty) {
            SecondaryChatSettings.save();
            settingsDirty = false;
        }
    }
}
