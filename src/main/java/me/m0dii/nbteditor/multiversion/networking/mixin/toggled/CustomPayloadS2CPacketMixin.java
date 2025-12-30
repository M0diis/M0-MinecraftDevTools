package me.m0dii.nbteditor.multiversion.networking.mixin.toggled;

import me.m0dii.nbteditor.multiversion.networking.MVPacketCustomPayload;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import me.m0dii.nbteditor.multiversion.networking.Networking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("deprecation")
@Mixin(CustomPayloadS2CPacket.class)
public class CustomPayloadS2CPacketMixin {
    @Inject(method = "method_53023(Lnet/minecraft/class_2960;Lnet/minecraft/class_2540;)Lnet/minecraft/class_8710;", at = @At("HEAD"), cancellable = true, remap = false)
    @SuppressWarnings("target")
    private static void readPayload(Identifier id, PacketByteBuf payload, CallbackInfoReturnable<CustomPayload> info) {
        ModPacket packet = Networking.readPacket(id, payload);
        if (packet != null) {
            info.setReturnValue(new MVPacketCustomPayload(packet));
        }
    }
}
