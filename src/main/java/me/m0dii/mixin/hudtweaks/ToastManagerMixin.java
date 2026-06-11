package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksModule;
import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public abstract class ToastManagerMixin {

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void m0dev$hudTweaks$beforeDraw(DrawContext context, CallbackInfo ci) {
        if (!HudTweaksModule.INSTANCE.isEnabled()) {
            return;
        }
        if (!HudTweaksSettings.getElement(HudTweaksSettings.ElementType.TOAST).display) {
            ci.cancel();
            return;
        }
        if (!HudTweaksRenderState.begin(HudTweaksSettings.ElementType.TOAST, context.getMatrices())) {
            ci.cancel();
        }
    }

    @Inject(method = "draw", at = @At("RETURN"))
    private void m0dev$hudTweaks$afterDraw(DrawContext context, CallbackInfo ci) {
        HudTweaksRenderState.end(context.getMatrices());
    }
}

