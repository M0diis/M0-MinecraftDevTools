package me.m0dii.mixin;

import me.m0dii.modules.debugdraw.DebugDrawManager;
import me.m0dii.modules.zoom.ZoomModule;
import me.m0dii.utils.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
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
        // GLFW press = 1
        if (action == 1) {
            CpsTracker.registerClick(input.getKeycode());

            if (DebugDrawManager.isSelectionEnabled()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.currentScreen == null) {
                    int key = input.getKeycode();
                    if (key == 0 && DebugDrawManager.pickSelectionPos(false)) {
                        ci.cancel();
                        return;
                    }
                    if (key == 1 && DebugDrawManager.pickSelectionPos(true)) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}
