package me.m0dii.mixin;

import me.m0dii.modules.reach.ReachModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerReachMixin {

    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void applyConfiguredBlockReach(CallbackInfoReturnable<Double> cir) {
        if (!ReachModule.INSTANCE.isEnabled()) {
            return;
        }

        double adjusted = ReachModule.INSTANCE.applyBlockReach(cir.getReturnValue(), isSingleplayerLike());
        cir.setReturnValue(adjusted);
    }

    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void applyConfiguredEntityReach(CallbackInfoReturnable<Double> cir) {
        if (!ReachModule.INSTANCE.isEnabled()) {
            return;
        }

        double adjusted = ReachModule.INSTANCE.applyEntityReach(cir.getReturnValue(), isSingleplayerLike());
        cir.setReturnValue(adjusted);
    }

    @Unique
    private static boolean isSingleplayerLike() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return true;
        }
        return client.isInSingleplayer() || client.isIntegratedServerRunning();
    }
}
