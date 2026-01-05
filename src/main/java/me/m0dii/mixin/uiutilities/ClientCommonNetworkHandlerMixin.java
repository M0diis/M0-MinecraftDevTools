package me.m0dii.mixin.uiutilities;

import me.m0dii.M0DevTools;
import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.MessageFormat;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow
    @Final
    protected MinecraftClient client;

    @Shadow
    public abstract void sendPacket(Packet<?> packet);

    @Inject(at = @At("HEAD"), method = "onResourcePackSend", cancellable = true)
    public void onResourcePackSend(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isBypassResourcePack() && (packet.required() || UiUtilitiesModule.INSTANCE.isResourcePackForceDeny())) {
            this.sendPacket(new ResourcePackStatusC2SPacket(MinecraftClient.getInstance().getSession().getUuidOrNull(),
                    ResourcePackStatusC2SPacket.Status.ACCEPTED));
            this.sendPacket(new ResourcePackStatusC2SPacket(MinecraftClient.getInstance().getSession().getUuidOrNull(),
                    ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));

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
