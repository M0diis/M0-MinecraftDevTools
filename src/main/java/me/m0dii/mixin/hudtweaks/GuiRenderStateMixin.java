package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {

    @ModifyArg(method = "addSpecialElement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;onElementAdded(Lnet/minecraft/client/gui/ScreenRect;)V"), index = 0, require = 0)
    private ScreenRect m0devHudTweaksScaleSpecialBounds(ScreenRect original) {
        if (!HudTweaksRenderState.compensateCoordinates()) {
            return original;
        }
        float scale = Math.max(0.01f, HudTweaksRenderState.currentScale());
        return new ScreenRect(
                Math.round(original.getLeft() / scale),
                Math.round(original.getTop() / scale),
                Math.round(original.width() / scale),
                Math.round(original.height() / scale)
        );
    }

    @ModifyArg(method = "addSpecialElement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;findAndGoToLayerToAdd(Lnet/minecraft/client/gui/render/state/GuiElementRenderState;)Z"), index = 0, require = 0)
    private GuiElementRenderState m0devHudTweaksStoreSpecialElement(GuiElementRenderState state) {
        return state;
    }
}

