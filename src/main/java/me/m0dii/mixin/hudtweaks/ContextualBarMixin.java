package me.m0dii.mixin.hudtweaks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ContextualBar.class)
public interface ContextualBarMixin {

    @ModifyExpressionValue(method = "left", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"), require = 0)
    private int m0devHudTweaksAdjustBarCenterX(int original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }

    @ModifyExpressionValue(method = "top", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"), require = 0)
    private int m0devHudTweaksAdjustBarCenterY(int original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }
}

