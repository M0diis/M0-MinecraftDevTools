package me.m0dii.mixin.hudtweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import me.m0dii.modules.hudtweaks.HudTweaksSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {
    @ModifyVariable(method = "fill(IIIII)V", at = @At("HEAD"), ordinal = 4, argsOnly = true, require = 0)
    private int m0devHudTweaksColorFill(int color) {
        return ARGB.multiplyAlpha(color, HudTweaksRenderState.currentOpacity());
    }

    @ModifyVariable(method = "fillGradient(IIIIII)V", at = @At("HEAD"), ordinal = 4, argsOnly = true, require = 0)
    private int m0devHudTweaksColorGradientA(int color) {
        return ARGB.multiplyAlpha(color, HudTweaksRenderState.currentOpacity());
    }

    @ModifyVariable(method = "fillGradient(IIIIII)V", at = @At("HEAD"), ordinal = 5, argsOnly = true, require = 0)
    private int m0devHudTweaksColorGradientB(int color) {
        return ARGB.multiplyAlpha(color, HudTweaksRenderState.currentOpacity());
    }

    @ModifyReturnValue(method = "guiWidth", at = @At("TAIL"), require = 0)
    private int m0devHudTweaksWidth(int original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }

    @ModifyReturnValue(method = "guiHeight", at = @At("TAIL"), require = 0)
    private int m0devHudTweaksHeight(int original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return Math.round(original / scale);
    }

    @ModifyVariable(
            method = "setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Z)V",
            at = @At("HEAD"),
            ordinal = 2,
            argsOnly = true,
            require = 0
    )
    private int m0devHudTweaksTooltipX(int original) {
        float scale = Math.max(0.01f, HudTweaksSettings.getElement(HudTweaksSettings.ElementType.TOOLTIP).scale);
        return Math.round(original / scale);
    }

    @ModifyVariable(
            method = "setTooltipForNextFrameInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Z)V",
            at = @At("HEAD"),
            ordinal = 3,
            argsOnly = true,
            require = 0
    )
    private int m0devHudTweaksTooltipY(int original) {
        float scale = Math.max(0.01f, HudTweaksSettings.getElement(HudTweaksSettings.ElementType.TOOLTIP).scale);
        return Math.round(original / scale);
    }
}
