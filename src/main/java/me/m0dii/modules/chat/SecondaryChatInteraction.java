package me.m0dii.modules.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import me.m0dii.mixin.ScreenClickEventInvoker;
import me.m0dii.modules.hudcanvas.HudCanvasDataHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;

public final class SecondaryChatInteraction {
    private static final double WHEEL_LINES_PER_NOTCH = 3.0;
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

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
    private static int lastMouseX;
    private static int lastMouseY;
    private static String selectedWindowId = "";
    private static String selectedTabId = "";
    private static long selectedMessageId = -1L;

    private static boolean hudCanvasDirty = false;
    private static boolean settingsDirty = false;
    private static Screen lastScreen = null;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.allowMouseClick(screen).register(SecondaryChatInteraction::allowMouseClick);
            ScreenMouseEvents.beforeMouseRelease(screen).register(SecondaryChatInteraction::handleMouseRelease);
            ScreenMouseEvents.allowMouseScroll(screen).register(SecondaryChatInteraction::allowMouseScroll);
            ScreenKeyboardEvents.allowKeyPress(screen).register(SecondaryChatInteraction::allowKeyPress);
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

    private static boolean allowMouseClick(Screen screen, Click click) {
        if (screen instanceof SecondaryChatConfigScreen) {
            return true;
        }

        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || !settings.showOverlay) {
            return true;
        }

        boolean leftClick = click.button() == 0;
        boolean rightClick = click.button() == 1;
        if (!leftClick && !rightClick) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;

        SecondaryChatWindowLayout.Frame menuFrame = openMenuFrame();
        if (menuFrame != null && SecondaryChatWindowLayout.containsMenu(menuFrame, mouseX, mouseY)) {
            if (leftClick) {
                handleMenuClick(menuFrame, mouseX, mouseY);
            }
            return false;
        }

        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (!frame.contains(mouseX, mouseY)) {
                continue;
            }

            if (SecondaryChatWindowLayout.containsSettingsButton(frame, mouseX, mouseY)) {
                if (leftClick) {
                    toggleMenu(frame.window.id);
                }
                return false;
            }

            openMenuWindowId = "";

            if (leftClick) {
                SecondaryChatWindowLayout.TabHit tabHit = SecondaryChatWindowLayout.tabAt(frame, mouseX, mouseY, client.textRenderer);
                if (tabHit != null) {
                    SecondaryChatManager.selectTab(tabHit.window.id, tabHit.tab.id);
                    clearSelectionForWindow(tabHit.window.id);
                    return false;
                }
            }

            SecondaryChatTextLayout.Hit hit = SecondaryChatTextLayout.hitAt(frame, client.textRenderer, mouseX, mouseY);
            if (hit != null) {
                selectMessage(frame, hit);
                if (rightClick) {
                    copyMessage(hit.line().message());
                } else {
                    handleStyledClick(screen, hit.style());
                }
                return false;
            }

            if (rightClick) {
                copySelectedMessage();
                return false;
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
            return false;
        }

        openMenuWindowId = "";
        return true;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;

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

    private static boolean allowMouseScroll(Screen screen,
                                          double mouseX,
                                          double mouseY,
                                          double horizontalAmount,
                                          double verticalAmount) {
        if (screen instanceof SecondaryChatConfigScreen) {
            return true;
        }

        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || !settings.showOverlay) {
            return true;
        }

        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;

        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (!frame.contains(mouseX, mouseY)) {
                continue;
            }

            SecondaryChatSettings.TabConfig tab = SecondaryChatManager.selectedTab(frame.window);
            if (tab == null) {
                return false;
            }
            double scrollAmount = verticalAmount * WHEEL_LINES_PER_NOTCH;
            SecondaryChatManager.scroll(frame.window.id, tab.id, scrollAmount);
            return false;
        }
        return true;
    }

    private static boolean allowKeyPress(Screen screen, KeyInput key) {
        if (screen instanceof SecondaryChatConfigScreen) {
            return true;
        }

        if (!SecondaryChatSettings.get().enabled || selectedMessageId < 0) {
            return true;
        }

        boolean copyShortcut = key.key() == InputUtil.GLFW_KEY_C
                && (key.modifiers() & (InputUtil.GLFW_MOD_CONTROL | InputUtil.GLFW_MOD_SUPER)) != 0;
        if (!copyShortcut || !isMouseOverSelectedWindow()) {
            return true;
        }

        return !copySelectedMessage();
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

    static int lastMouseX() {
        return lastMouseX;
    }

    static int lastMouseY() {
        return lastMouseY;
    }

    static boolean isSelected(String windowId, String tabId, SecondaryChatManager.ChatMessage message) {
        return message != null
                && selectedMessageId == message.id()
                && selectedWindowId.equals(windowId)
                && selectedTabId.equals(tabId);
    }

    static SecondaryChatTextLayout.Hit hoveredMessageHit(SecondaryChatWindowLayout.Frame frame,
                                                         net.minecraft.client.font.TextRenderer textRenderer) {
        if (frame == null || !isTopmostWindowAtMouse(frame.window.id)) {
            return null;
        }
        if (isMenuOpen(frame.window.id) && SecondaryChatWindowLayout.containsMenu(frame, lastMouseX, lastMouseY)) {
            return null;
        }
        if (SecondaryChatWindowLayout.containsSettingsButton(frame, lastMouseX, lastMouseY)) {
            return null;
        }
        return SecondaryChatTextLayout.hitAt(frame, textRenderer, lastMouseX, lastMouseY);
    }

    static String menuLabel(int row) {
        return switch (row) {
            case 0 -> "New Tab";
            case 1 -> "Delete Tab";
            case 2 -> "Copy JSON";
            case 3 -> "Clear Tab";
            case 4 -> "Clear Window";
            case 5 -> "Editor";
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
            case 2 -> selectedMessage(frame.window.id) != null;
            case 3 -> selected != null;
            case 4 -> true;
            case 5 -> true;
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
            case 2 -> copySelectedMessageJson(frame.window.id);
            case 3 -> clearSelectedTab(frame.window);
            case 4 -> {
                SecondaryChatManager.clearWindow(frame.window.id);
                clearSelectionForWindow(frame.window.id);
            }
            case 5 -> openEditor();
            default -> {
                return;
            }
        }

        if (row != 5) {
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
        clearSelectionForTab(windowId, removedTabId);
    }

    private static void clearSelectedTab(SecondaryChatSettings.WindowConfig window) {
        SecondaryChatSettings.TabConfig tab = SecondaryChatManager.selectedTab(window);
        if (tab != null) {
            SecondaryChatManager.clear(window.id, tab.id);
            clearSelectionForTab(window.id, tab.id);
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

    private static void selectMessage(SecondaryChatWindowLayout.Frame frame, SecondaryChatTextLayout.Hit hit) {
        SecondaryChatSettings.TabConfig tab = SecondaryChatManager.selectedTab(frame.window);
        if (tab == null || hit == null || hit.line() == null) {
            clearSelection();
            return;
        }

        selectedWindowId = frame.window.id;
        selectedTabId = tab.id;
        selectedMessageId = hit.line().message().id();
    }

    private static void handleStyledClick(Screen screen, Style style) {
        if (style == null) {
            return;
        }

        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            ScreenClickEventInvoker.invokeHandleClickEvent(clickEvent, client, screen);
        }
    }

    private static boolean copySelectedMessage() {
        SecondaryChatManager.ChatMessage selected = selectedMessage();
        if (selected == null) {
            clearSelection();
            return false;
        }
        copyMessage(selected);
        return true;
    }

    private static void copyMessage(SecondaryChatManager.ChatMessage message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || message == null) {
            return;
        }

        client.keyboard.setClipboard(message.plainText());
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Copied chat message").formatted(Formatting.GRAY), true);
        }
    }

    private static SecondaryChatManager.ChatMessage selectedMessage() {
        if (selectedMessageId < 0 || selectedWindowId.isEmpty() || selectedTabId.isEmpty()) {
            return null;
        }
        return selectedMessage(selectedWindowId);
    }

    private static SecondaryChatManager.ChatMessage selectedMessage(String windowId) {
        if (selectedMessageId < 0 || selectedTabId.isEmpty() || !selectedWindowId.equals(windowId)) {
            return null;
        }
        for (SecondaryChatManager.ChatMessage message : SecondaryChatManager.snapshot(selectedWindowId, selectedTabId)) {
            if (message.id() == selectedMessageId) {
                return message;
            }
        }
        return null;
    }

    private static void copySelectedMessageJson(String windowId) {
        SecondaryChatManager.ChatMessage selected = selectedMessage(windowId);
        if (selected == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.keyboard.setClipboard(toComponentJson(selected.text()));
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Copied chat component JSON").formatted(Formatting.GRAY), true);
        }
    }

    private static String toComponentJson(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        RegistryWrapper.WrapperLookup registryLookup = client == null || client.world == null
                ? null
                : client.world.getRegistryManager();

        return TextCodecs.CODEC.encodeStart(
                        registryLookup == null ? JsonOps.INSTANCE : registryLookup.getOps(JsonOps.INSTANCE),
                        text
                )
                .result()
                .map(SecondaryChatInteraction::prettyJson)
                .orElseGet(() -> PRETTY_GSON.toJson(text.getString()));
    }

    private static String prettyJson(JsonElement element) {
        return PRETTY_GSON.toJson(element);
    }

    private static boolean isMouseOverSelectedWindow() {
        if (selectedWindowId.isEmpty()) {
            return false;
        }
        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (frame.window.id.equals(selectedWindowId) && frame.contains(lastMouseX, lastMouseY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTopmostWindowAtMouse(String windowId) {
        for (SecondaryChatWindowLayout.Frame frame : SecondaryChatWindowLayout.framesTopFirst()) {
            if (frame.contains(lastMouseX, lastMouseY)) {
                return frame.window.id.equals(windowId);
            }
        }
        return false;
    }

    private static void clearSelectionForWindow(String windowId) {
        if (selectedWindowId.equals(windowId)) {
            clearSelection();
        }
    }

    private static void clearSelectionForTab(String windowId, String tabId) {
        if (selectedWindowId.equals(windowId) && selectedTabId.equals(tabId)) {
            clearSelection();
        }
    }

    private static void clearSelection() {
        selectedWindowId = "";
        selectedTabId = "";
        selectedMessageId = -1L;
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
