package me.m0dii.nbteditor.multiversion.networking.mixin;

import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.multiversion.networking.MVPacketCustomPayload;
import me.m0dii.nbteditor.multiversion.networking.MVServerNetworking;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import me.m0dii.nbteditor.server.NBTEditorServer;
import me.m0dii.nbteditor.server.ServerMixinLink;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("deprecation")
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Shadow
    private PacketListener packetListener;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void handlePacket(Packet<?> packet, PacketListener listener, CallbackInfo info) {
        if (!NBTEditorServer.IS_DEDICATED && ServerMixinLink.isInstanceOfClientPlayNetworkHandlerSafely(listener) && packet instanceof CustomPayloadS2CPacket customPacket) {
            ModPacket modPacket = MVPacketCustomPayload.unwrapS2C(customPacket);

            if (modPacket != null) {
                ClientNetworking.callListeners(modPacket);
                info.cancel();
            }
        }
        if (listener instanceof ServerPlayNetworkHandler handler && packet instanceof CustomPayloadC2SPacket customPacket) {
            ModPacket modPacket = MVPacketCustomPayload.unwrapC2S(customPacket);

            if (modPacket != null) {
                MVServerNetworking.callListeners(modPacket, handler.player);
                info.cancel();
            }
        }
    }

    @Shadow
    public abstract boolean isOpen();

    @Inject(method = "disconnect*", at = @At("HEAD"))
    private void disconnect(Text reason, CallbackInfo info) {
        if (isOpen()) {
            if (!NBTEditorServer.IS_DEDICATED && ServerMixinLink.isInstanceOfClientPlayNetworkHandlerSafely(packetListener)) {
                ClientNetworking.onPlayStop();
            }

            if (packetListener instanceof ServerPlayNetworkHandler handler) {
                MVServerNetworking.onPlayStop(handler.player);
            }
        }
    }
}
