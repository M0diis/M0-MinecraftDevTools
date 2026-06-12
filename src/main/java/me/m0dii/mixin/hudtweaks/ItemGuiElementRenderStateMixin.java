package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.ItemGuiElementRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemGuiElementRenderState.class)
public abstract class ItemGuiElementRenderStateMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/ItemGuiElementRenderState;createBounds(Lnet/minecraft/client/gui/ScreenRect;)Lnet/minecraft/client/gui/ScreenRect;"), index = 0, require = 0)
    private ScreenRect m0devHudTweaksScaleItemBounds(ScreenRect original) {
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
}

