package me.m0dii.mixin;

import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "getGameMode", at = @At("HEAD"), cancellable = true)
    private void overrideIsSpectator(CallbackInfoReturnable<GameMode> cir) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(GameMode.SPECTATOR);
        }
    }

}
