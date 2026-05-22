package me.m0dii.mixin;

import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels server-side position correction packets while Freecam is active.
 * Without this, the server periodically "rubber-bands" the client player
 * back to its server-authoritative position, causing the viewpoint to snap.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerFreecamMixin {

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"), cancellable = true)
    private void cancelPositionLookInFreecam(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            ci.cancel();
        }
    }
}

