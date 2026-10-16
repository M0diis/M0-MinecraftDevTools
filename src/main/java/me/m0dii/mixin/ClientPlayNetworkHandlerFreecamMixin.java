package me.m0dii.mixin;

import me.m0dii.modules.camera.CameraPathManager;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps freecam/network state in sync across respawns and dimension changes.
 */
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerFreecamMixin {

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void disableFreecamOnRespawn(ClientboundRespawnPacket packet, CallbackInfo ci) {
        if (CameraEntity.hasController()) {
            CameraPathManager.onClientWorldUnavailable();
            CameraEntity.clearController();
        }
        if (FreecamModule.INSTANCE.isEnabled()) {
            FreecamModule.INSTANCE.setEnabled(false);
        }
    }
}

