package me.m0dii.mixin.packets;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import me.m0dii.modules.packets.PacketInspectorModule;
import me.m0dii.modules.packets.PacketLog;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds the packet inspector. Does nothing unless the inspector is recording. */
@Mixin(Connection.class)
public class PacketInspectorConnectionMixin {

    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void m0dev$logIncoming(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (PacketInspectorModule.LOG.isRecording()) {
            PacketInspectorModule.record((Connection) (Object) this, PacketLog.Direction.IN, packet);
        }
    }

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void m0dev$logOutgoing(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (PacketInspectorModule.LOG.isRecording()) {
            PacketInspectorModule.record((Connection) (Object) this, PacketLog.Direction.OUT, packet);
        }
    }
}
