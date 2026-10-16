package me.m0dii.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.debugdraw.DebugDrawManager;
import me.m0dii.modules.zoom.ZoomModule;
import me.m0dii.utils.CpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        ZoomModule.INSTANCE.onScroll(horizontal, vertical);
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (action == InputConstants.PRESS) {
            CpsTracker.registerClick(input.input());

            int key = input.input();
            if ((key == InputConstants.MOUSE_BUTTON_LEFT || key == InputConstants.MOUSE_BUTTON_RIGHT)
                    && DebugDrawManager.shouldCaptureSelectionClick()) {
                Minecraft client = Minecraft.getInstance();
                boolean rightClick = key == InputConstants.MOUSE_BUTTON_RIGHT;
                if (DebugDrawManager.pickSelectionPos(rightClick)) {
                    if (client != null && client.player != null) {
                        client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("[Draw] Set " + (rightClick ? "pos2" : "pos1") + "."));
                    }
                    ci.cancel();
                } else if (client != null && client.player != null) {
                    client.player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("[Draw] No block under crosshair."));
                }
            }
        }
    }
}
