package me.m0dii.mixin;

import me.m0dii.modules.reach.ReachModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerReachMixin {

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void applyConfiguredBlockReach(CallbackInfoReturnable<Double> cir) {
        if (!ReachModule.INSTANCE.isEnabled()) {
            return;
        }

        double adjusted = ReachModule.INSTANCE.applyBlockReach(cir.getReturnValue(), isSingleplayerLike());
        cir.setReturnValue(adjusted);
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void applyConfiguredEntityReach(CallbackInfoReturnable<Double> cir) {
        if (!ReachModule.INSTANCE.isEnabled()) {
            return;
        }

        double adjusted = ReachModule.INSTANCE.applyEntityReach(cir.getReturnValue(), isSingleplayerLike());
        cir.setReturnValue(adjusted);
    }

    @Unique
    private static boolean isSingleplayerLike() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return true;
        }
        return client.isLocalServer() || client.hasSingleplayerServer();
    }
}
