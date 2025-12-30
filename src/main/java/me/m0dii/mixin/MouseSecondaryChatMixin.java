package me.m0dii.mixin;

import me.m0dii.modules.chat.SecondaryChatInteraction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseSecondaryChatMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onCursorPos", at = @At("TAIL"))
    private void onCursorMove(long window, double x, double y, CallbackInfo ci) {
        if (client.currentScreen != null) {
            double mouseX = x * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double mouseY = y * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            SecondaryChatInteraction.handleMouseMove(mouseX, mouseY);
        }
    }
}

