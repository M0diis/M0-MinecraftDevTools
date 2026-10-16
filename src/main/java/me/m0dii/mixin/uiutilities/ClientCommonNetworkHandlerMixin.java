package me.m0dii.mixin.uiutilities;

import me.m0dii.M0DevTools;
import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.MessageFormat;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(at = @At("HEAD"), method = "handleResourcePackPush", cancellable = true)
    public void onResourcePackSend(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isBypassResourcePack() && (packet.required() || UiUtilitiesModule.INSTANCE.isResourcePackForceDeny())) {
            this.send(new ServerboundResourcePackPacket(Minecraft.getInstance().getUser().getProfileId(),
                    ServerboundResourcePackPacket.Action.ACCEPTED));
            this.send(new ServerboundResourcePackPacket(Minecraft.getInstance().getUser().getProfileId(),
                    ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));

            String message = MessageFormat.format("[M0-Dev-Tools]: Resource Pack bypassed, message: {0}, URL: {1}",
                    packet.prompt().isEmpty()
                            ? "<EMPTY>"
                            : packet.prompt().toString(),
                    packet.url() == null
                            ? "<EMPTY>"
                            : packet.url());

            M0DevTools.LOGGER.info(message);

            ci.cancel();
        }
    }
}
