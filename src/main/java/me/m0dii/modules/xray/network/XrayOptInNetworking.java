package me.m0dii.modules.xray.network;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class XrayOptInNetworking {
    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "xray/server_opt_in");

    private static boolean receiversRegistered = false;

    private XrayOptInNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(XrayOptInProbePayload.ID, XrayOptInProbePayload.CODEC);
    }

    public static void registerReceivers() {
        if (receiversRegistered) {
            return;
        }
        receiversRegistered = true;

        // Receiver existence is enough for the client handshake check (ClientPlayNetworking.canSend).
        ServerPlayNetworking.registerGlobalReceiver(XrayOptInProbePayload.ID, (payload, context) -> {
        });
    }

    public record XrayOptInProbePayload(boolean probe) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<XrayOptInProbePayload> ID = new CustomPacketPayload.Type<>(CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, XrayOptInProbePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new XrayOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}

