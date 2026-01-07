package me.m0dii.modules.chat;

import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class SecondaryChatInteraction {
    private SecondaryChatInteraction() {
    }

    private static boolean dragging = false;
    private static boolean resizing = false;
    private static int dragStartX;
    private static int dragStartY;
    private static int dragOffsetX;
    private static int dragOffsetY;

    private static boolean configDirty = false;

    private static Screen lastScreen = null;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.beforeMouseClick(screen).register(SecondaryChatInteraction::handleMouseClick);
            ScreenMouseEvents.beforeMouseRelease(screen).register(SecondaryChatInteraction::handleMouseRelease);
            ScreenMouseEvents.beforeMouseScroll(screen).register(SecondaryChatInteraction::handleMouseScroll);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.currentScreen != lastScreen) {
                if (configDirty) {
                    ModConfig.save();
                    configDirty = false;
                }

                if (client.currentScreen == null) {
                    dragging = false;
                    resizing = false;
                }

                lastScreen = client.currentScreen;
            }
        });
    }

    private static void handleMouseClick(Screen screen, double mouseX, double mouseY, int button) {
        if (!ModConfig.secondaryChatEnabled || !ModConfig.secondaryChatShowOverlay) {
            return;
        }

        if (button != 0) {
            return; // Left
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        int x = ModConfig.secondaryChatX;
        int y = ModConfig.secondaryChatY;
        int w = Math.max(50, ModConfig.secondaryChatWidth);
        int h = Math.max(30, ModConfig.secondaryChatHeight);

        // Check if clicking in the secondary chat box area
        if (!isInside(mouseX, mouseY, x, y, w, h)) {
            return;
        }

        // Check for resize handle
        int resizeX = x + w - 6;
        int resizeY = y + h - 6;
        if (isInside(mouseX, mouseY, resizeX, resizeY, 6, 6)) {
            resizing = true;
            dragging = false;
            dragStartX = (int) mouseX;
            dragStartY = (int) mouseY;
            dragOffsetX = w;
            dragOffsetY = h;
        } else {
            dragging = true;
            resizing = false;
            dragStartX = (int) mouseX;
            dragStartY = (int) mouseY;
            dragOffsetX = x;
            dragOffsetY = y;
        }
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        int x = ModConfig.secondaryChatX;
        int y = ModConfig.secondaryChatY;
        int w = Math.max(50, ModConfig.secondaryChatWidth);
        int h = Math.max(30, ModConfig.secondaryChatHeight);


        if (!dragging && !resizing) {
            if (ModConfig.resetTransparencyWhenHovered && isInside(mouseX, mouseY, x, y, w, h)) {
                SecondaryChatManager.setLastAlphaReset(System.currentTimeMillis());
            }
            return;
        }

        int mx = (int) mouseX;
        int my = (int) mouseY;
        int dx = mx - dragStartX;
        int dy = my - dragStartY;

        if (dragging) {
            ModConfig.secondaryChatX = dragOffsetX + dx;
            ModConfig.secondaryChatY = dragOffsetY + dy;
            configDirty = true;
        } else if (resizing) {
            ModConfig.secondaryChatWidth = Math.max(100, dragOffsetX + dx);
            ModConfig.secondaryChatHeight = Math.max(50, dragOffsetY + dy);
            configDirty = true;
        }
    }

    private static void handleMouseRelease(Screen screen, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return;
        }
        if (!dragging && !resizing) {
            return;
        }

        dragging = false;
        resizing = false;

        // Persist any pending changes once the user finishes interacting
        if (configDirty) {
            ModConfig.save();
            configDirty = false;
        }
    }

    private static void handleMouseScroll(Screen screen, double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!ModConfig.secondaryChatEnabled || !ModConfig.secondaryChatShowOverlay) {
            return;
        }

        int x = ModConfig.secondaryChatX;
        int y = ModConfig.secondaryChatY;
        int w = Math.max(50, ModConfig.secondaryChatWidth);
        int h = Math.max(30, ModConfig.secondaryChatHeight);

        if (!isInside(mouseX, mouseY, x, y, w, h)) {
            return;
        }

        int scrollAmount = (int) Math.signum(verticalAmount);
        SecondaryChatOverlay.scroll(scrollAmount);

    }

    public static boolean isDraggingOrResizing() {
        return dragging || resizing;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
