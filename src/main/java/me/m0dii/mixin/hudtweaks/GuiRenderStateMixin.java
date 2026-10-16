package me.m0dii.mixin.hudtweaks;

import me.m0dii.modules.hudtweaks.HudTweaksRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin {

    @ModifyArg(method = "addPicturesInPictureState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addDebugRectangleIfEnabled(Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 0, require = 0)
    private ScreenRectangle m0devHudTweaksScaleSpecialBounds(ScreenRectangle original) {
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

    @ModifyArg(method = "addPicturesInPictureState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;findAppropriateNode(Lnet/minecraft/client/renderer/state/gui/ScreenArea;)Z"), index = 0, require = 0)
    private ScreenArea m0devHudTweaksStoreSpecialElement(ScreenArea state) {
        return state;
    }
}

