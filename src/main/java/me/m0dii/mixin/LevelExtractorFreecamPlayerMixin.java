package me.m0dii.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla skips every {@code LocalPlayer} that is not the camera entity. The freecam camera is itself a
 * {@code LocalPlayer}, so without this the real player is never drawn while freecam is on.
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorFreecamPlayerMixin {

    @ModifyExpressionValue(method = "extractVisibleEntities",
            at = @At(value = "CONSTANT", args = "classValue=net/minecraft/client/player/LocalPlayer"))
    private boolean m0dev$renderPlayerInFreecam(boolean isLocalPlayer) {
        if (isLocalPlayer && FreecamModule.INSTANCE.isEnabled() && FreecamModule.INSTANCE.shouldShowPlayerModel()) {
            return false;
        }
        return isLocalPlayer;
    }
}
