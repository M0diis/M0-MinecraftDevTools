package me.m0dii.mixin.uiutilities;

import io.netty.channel.ChannelFutureListener;
import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ClientConnectionMixin {

    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    public void sendImmediately(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!UiUtilitiesModule.INSTANCE.isSendUiPackets() && (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerButtonClickPacket)) {
            ci.cancel();
            return;
        }

        if (UiUtilitiesModule.INSTANCE.isDelayUiPackets() && (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundContainerButtonClickPacket)) {
            UiUtilitiesModule.INSTANCE.getDelayedUiPackets().add(packet);
            ci.cancel();
        }

        if (!UiUtilitiesModule.INSTANCE.isShouldEditSign() && (packet instanceof ServerboundSignUpdatePacket)) {
            UiUtilitiesModule.INSTANCE.setShouldEditSign(true);
            ci.cancel();
        }
    }
}
