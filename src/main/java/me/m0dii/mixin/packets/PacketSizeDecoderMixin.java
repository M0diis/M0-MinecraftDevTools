package me.m0dii.mixin.packets;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.m0dii.modules.packets.PacketInspectorModule;
import me.m0dii.modules.packets.PacketLog;
import net.minecraft.network.PacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Measures incoming packet sizes for the packet inspector: one frame (already decompressed) is decoded per call.
 * {@code require = 0}: if the method ever changes, sizes just stay unknown instead of crashing.
 */
@Mixin(PacketDecoder.class)
public class PacketSizeDecoderMixin {
    @Unique
    private static final ThreadLocal<int[]> M0DEV$SIZE = ThreadLocal.withInitial(() -> new int[]{-1});

    @Inject(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V",
            at = @At("HEAD"), require = 0)
    private void m0dev$measure(ChannelHandlerContext context, ByteBuf in, List<Object> out, CallbackInfo ci) {
        M0DEV$SIZE.get()[0] = PacketInspectorModule.LOG.isRecording() ? in.readableBytes() : -1;
    }

    @Inject(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V",
            at = @At("RETURN"), require = 0)
    private void m0dev$report(ChannelHandlerContext context, ByteBuf in, List<Object> out, CallbackInfo ci) {
        int size = M0DEV$SIZE.get()[0];
        M0DEV$SIZE.get()[0] = -1;
        if (size < 0 || out.isEmpty()) {
            return;
        }
        // Only what the server sends us (a LAN host also decodes its guests' packets).
        if (PacketLog.nameOf(out.getLast().getClass()).startsWith("Clientbound")) {
            PacketInspectorModule.noteDecodedSize(size);
        }
    }
}
