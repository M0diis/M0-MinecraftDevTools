package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiTextRenderState.class)
public abstract class TextGuiElementRenderStateMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 2, argsOnly = true, require = 0)
    private static int m0devHudTweaksTextColor(int original) {
        return ARGB.multiplyAlpha(original, HudTweaksRenderState.currentOpacity());
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 3, argsOnly = true, require = 0)
    private static int m0devHudTweaksTextBgColor(int original) {
        return ARGB.multiplyAlpha(original, HudTweaksRenderState.currentOpacity());
    }
}

