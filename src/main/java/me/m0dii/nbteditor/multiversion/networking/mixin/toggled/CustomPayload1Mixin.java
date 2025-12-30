package me.m0dii.nbteditor.multiversion.networking.mixin.toggled;

import me.m0dii.nbteditor.multiversion.networking.MVPacketCustomPayload;
import me.m0dii.nbteditor.multiversion.networking.Networking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("deprecation")
@Mixin(targets = "net.minecraft.network.packet.CustomPayload$1")
public class CustomPayload1Mixin {
    @Inject(method = "getCodec", at = @At("HEAD"), cancellable = true)
    private void getCodec(Identifier id, CallbackInfoReturnable<PacketCodec<PacketByteBuf, MVPacketCustomPayload>> info) {
        if (!Networking.isPacket(id)) {
            return;
        }
        info.setReturnValue(PacketCodec.of((packet, payload) -> packet.packet().write(payload),
                payload -> new MVPacketCustomPayload(Networking.readPacket(id, payload))));
    }
}
