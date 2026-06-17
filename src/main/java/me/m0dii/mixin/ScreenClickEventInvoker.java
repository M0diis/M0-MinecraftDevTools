package me.m0dii.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenClickEventInvoker {
    @Invoker("handleClickEvent")
    static void invokeHandleClickEvent(ClickEvent clickEvent, MinecraftClient client, Screen screen) {
        throw new AssertionError();
    }
}
