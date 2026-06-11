package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksModule;
import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubtitlesHud.class)
public abstract class SubtitlesHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void m0dev$hudTweaks$beforeRender(DrawContext context, CallbackInfo ci) {
        if (!HudTweaksModule.INSTANCE.isEnabled()) {
            return;
        }
        if (!HudTweaksSettings.getElement(HudTweaksSettings.ElementType.SUBTITLES).display) {
            ci.cancel();
            return;
        }
        if (!HudTweaksRenderState.begin(HudTweaksSettings.ElementType.SUBTITLES, context.getMatrices())) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void m0dev$hudTweaks$afterRender(DrawContext context, CallbackInfo ci) {
        HudTweaksRenderState.end(context.getMatrices());
    }
}

