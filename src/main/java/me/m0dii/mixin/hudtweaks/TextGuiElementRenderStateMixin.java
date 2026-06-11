package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextGuiElementRenderState.class)
public abstract class TextGuiElementRenderStateMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 2, argsOnly = true, require = 0)
    private static int m0devHudTweaksTextColor(int original) {
        return ColorHelper.scaleAlpha(original, HudTweaksRenderState.currentOpacity());
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 3, argsOnly = true, require = 0)
    private static int m0devHudTweaksTextBgColor(int original) {
        return ColorHelper.scaleAlpha(original, HudTweaksRenderState.currentOpacity());
    }
}

