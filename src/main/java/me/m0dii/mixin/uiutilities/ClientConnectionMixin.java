package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(at = @At("HEAD"), method = "sendImmediately", cancellable = true)
    public void sendImmediately(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (!UiUtilitiesModule.INSTANCE.isSendUiPackets() && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket)) {
            ci.cancel();
            return;
        }

        if (UiUtilitiesModule.INSTANCE.isDelayUiPackets() && (packet instanceof ClickSlotC2SPacket || packet instanceof ButtonClickC2SPacket)) {
            UiUtilitiesModule.INSTANCE.getDelayedUiPackets().add(packet);
            ci.cancel();
        }

        if (!UiUtilitiesModule.INSTANCE.isShouldEditSign() && (packet instanceof UpdateSignC2SPacket)) {
            UiUtilitiesModule.INSTANCE.setShouldEditSign(true);
            ci.cancel();
        }
    }
}
