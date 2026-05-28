package me.m0dii.mixin;

import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the camera entity from sending movement packets to the server,
 * and prevents the real player from updating its position while freecam is active.
 * Without this, position sync packets cause the camera view to rubberband.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityFreecamMixin {

    /**
     * Suppress sendMovementPackets for two cases:
     * 1. The CameraEntity itself — it must never send its position to the server.
     * 2. The real player while freecam is active — the DummyMovementInput already
     *    stops movement, but sendMovementPackets would still broadcast the frozen
     *    position every tick and trigger server-side rubber-band corrections.
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void suppressMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        // Always block the camera entity from sending its position to the server.
        if (self == CameraEntity.getCamera()) {
            ci.cancel();
            return;
        }
        // While freecam is active, also block the real player so the server
        // does not rubber-band the camera view back.
        if (FreecamModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}

