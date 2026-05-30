package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents only the detached camera entity from sending movement packets.
 * The real player must keep normal packet flow to stay network-synced.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityFreecamMixin {

    /**
     * Suppress sendMovementPackets for the CameraEntity itself.
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void suppressMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (self == CameraEntity.getCamera()) {
            ci.cancel();
        }
    }
}

