package me.m0dii.mixin.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.m0dii.modules.packets.PacketInspectorModule;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Measures outgoing packet sizes (before compression) for the packet inspector. Optional like the decoder hook. */
@Mixin(PacketEncoder.class)
public class PacketSizeEncoderMixin {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
            at = @At("RETURN"), require = 0)
    private void m0dev$report(ChannelHandlerContext context, Packet<?> packet, ByteBuf out, CallbackInfo ci) {
        if (PacketInspectorModule.LOG.isRecording()) {
            PacketInspectorModule.noteEncodedSize(packet, out.readableBytes());
        }
    }
}
