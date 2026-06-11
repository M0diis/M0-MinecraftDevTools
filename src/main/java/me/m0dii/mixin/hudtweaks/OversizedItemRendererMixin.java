package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.render.OversizedItemGuiElementRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(OversizedItemGuiElementRenderer.class)
public abstract class OversizedItemRendererMixin {
    @ModifyVariable(method = "getYOffset", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private int m0devHudTweaksOversizedYOffset(int original) {
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }
}
