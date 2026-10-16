package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiItemRenderState.class)
public abstract class ItemGuiElementRenderStateMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;calculateBounds(Lnet/minecraft/client/gui/navigation/ScreenRectangle;)Lnet/minecraft/client/gui/navigation/ScreenRectangle;"), index = 0, require = 0)
    private ScreenRectangle m0devHudTweaksScaleItemBounds(ScreenRectangle original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return new ScreenRectangle(
                Math.round(original.left() / scale),
                Math.round(original.top() / scale),
                Math.round(original.width() / scale),
                Math.round(original.height() / scale)
        );
    }
}

