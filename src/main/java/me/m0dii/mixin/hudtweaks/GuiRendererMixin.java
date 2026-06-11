package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @ModifyVariable(method = "prepareItem(Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;FFII)V", at = @At("HEAD"), ordinal = 1, argsOnly = true, require = 0)
    private float m0devHudTweaksItemX(float original) {
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return original / scale;
    }

    @ModifyVariable(method = "prepareItem(Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;FFII)V", at = @At("HEAD"), ordinal = 2, argsOnly = true, require = 0)
    private float m0devHudTweaksItemY(float original) {
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return original / scale;
    }
}
