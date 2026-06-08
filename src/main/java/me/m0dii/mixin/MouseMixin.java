package me.m0dii.mixin;

import me.m0dii.modules.debugdraw.DebugDrawManager;
import me.m0dii.modules.zoom.ZoomModule;
import me.m0dii.utils.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        ZoomModule.INSTANCE.onScroll(horizontal, vertical);
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS) {
            CpsTracker.registerClick(input.getKeycode());

            int key = input.getKeycode();
            if ((key == GLFW.GLFW_MOUSE_BUTTON_LEFT || key == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                    && DebugDrawManager.shouldCaptureSelectionClick()) {
                MinecraftClient client = MinecraftClient.getInstance();
                boolean rightClick = key == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
                if (DebugDrawManager.pickSelectionPos(rightClick)) {
                    if (client != null && client.player != null) {
                        client.player.sendMessage(net.minecraft.text.Text.literal("[Draw] Set " + (rightClick ? "pos2" : "pos1") + "."), true);
                    }
                    ci.cancel();
                } else if (client != null && client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.literal("[Draw] No block under crosshair."), true);
                }
            }
        }
    }
}
