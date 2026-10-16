package me.m0dii.gui.theme;

import net.minecraft.client.Minecraft;

/**
 * Marker for screens whose vanilla buttons and text fields are drawn in the flat {@link Theme} style.
 */
public interface ThemedScreen {

    static boolean isActive() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.gui != null && client.gui.screen() instanceof ThemedScreen;
    }
}
