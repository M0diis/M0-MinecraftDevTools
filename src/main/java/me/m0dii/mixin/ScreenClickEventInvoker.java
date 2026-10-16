package me.m0dii.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenClickEventInvoker {
    @Invoker("defaultHandleGameClickEvent")
    static void invokeHandleClickEvent(ClickEvent clickEvent, Minecraft client, Screen screen) {
        throw new AssertionError();
    }
}
