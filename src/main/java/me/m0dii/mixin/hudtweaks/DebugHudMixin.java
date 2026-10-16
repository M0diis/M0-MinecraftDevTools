package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksModule;
import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void m0dev$hudTweaks$beforeRender(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (!HudTweaksModule.INSTANCE.isEnabled()) {
            return;
        }
        if (!HudTweaksSettings.getElement(HudTweaksSettings.ElementType.DEBUG_SCREEN).display) {
            ci.cancel();
            return;
        }
        if (!HudTweaksRenderState.begin(HudTweaksSettings.ElementType.DEBUG_SCREEN, context.pose())) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void m0dev$hudTweaks$afterRender(GuiGraphicsExtractor context, CallbackInfo ci) {
        HudTweaksRenderState.end(context.pose());
    }
}


