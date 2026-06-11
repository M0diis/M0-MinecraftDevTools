package me.m0dii.modules.xray.network;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class XrayOptInNetworking {
    public static final Identifier CHANNEL_ID = Identifier.of(M0DevToolsClient.MOD_ID, "xray/server_opt_in");

    private static boolean receiversRegistered = false;

    private XrayOptInNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(XrayOptInProbePayload.ID, XrayOptInProbePayload.CODEC);
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

    public record XrayOptInProbePayload(boolean probe) implements CustomPayload {
        public static final CustomPayload.Id<XrayOptInProbePayload> ID = new CustomPayload.Id<>(CHANNEL_ID);
        public static final PacketCodec<RegistryByteBuf, XrayOptInProbePayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new XrayOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}

