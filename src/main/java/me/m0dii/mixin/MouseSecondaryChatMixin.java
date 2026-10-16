package me.m0dii.mixin;

import me.m0dii.modules.chat.SecondaryChatInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseSecondaryChatMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onMove", at = @At("TAIL"))
    private void onCursorMove(long window, double x, double y, double xRel, double yRel, CallbackInfo ci) {
        if (minecraft.gui.screen() != null) {
            double mouseX = x * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
            double mouseY = y * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
            SecondaryChatInteraction.handleMouseMove(mouseX, mouseY);
        }
    }
}

