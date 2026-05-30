package me.m0dii.mixin;

import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps freecam/network state in sync across teleports and respawns. */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerFreecamMixin {

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    private void disableFreecamOnServerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            FreecamModule.INSTANCE.setEnabled(false);
        }
    }

    @Inject(method = "onPlayerRespawn", at = @At("HEAD"))
    private void disableFreecamOnRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            FreecamModule.INSTANCE.setEnabled(false);
        }
    }
}

