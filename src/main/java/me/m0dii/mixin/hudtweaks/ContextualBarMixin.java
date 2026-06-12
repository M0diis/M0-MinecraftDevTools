package me.m0dii.mixin.hudtweaks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.hud.bar.Bar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Bar.class)
public interface ContextualBarMixin {

    @ModifyExpressionValue(method = "getCenterX", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getScaledWidth()I"), require = 0)
    private int m0devHudTweaksAdjustBarCenterX(int original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }

    @ModifyExpressionValue(method = "getCenterY", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getScaledHeight()I"), require = 0)
    private int m0devHudTweaksAdjustBarCenterY(int original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }
}

