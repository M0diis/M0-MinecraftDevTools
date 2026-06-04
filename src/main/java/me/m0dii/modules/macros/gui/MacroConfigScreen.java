package me.m0dii.modules.macros.gui;

import net.minecraft.client.gui.screen.Screen;

public final class MacroConfigScreen {
    private MacroConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return MacroWorkbenchScreen.create(parent, MacroWorkbenchScreen.Tab.MACROS);
    }
}
