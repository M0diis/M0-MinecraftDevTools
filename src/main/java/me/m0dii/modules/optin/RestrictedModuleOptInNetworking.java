package me.m0dii.modules.optin;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class RestrictedModuleOptInNetworking {
    public static final Identifier INSTANT_BREAK_CHANNEL_ID = Identifier.of(M0DevToolsClient.MOD_ID, "instant_break/server_opt_in");
    public static final Identifier REACH_CHANNEL_ID = Identifier.of(M0DevToolsClient.MOD_ID, "reach/server_opt_in");
    public static final Identifier FREECAM_CHANNEL_ID = Identifier.of(M0DevToolsClient.MOD_ID, "freecam/server_opt_in");
    public static final Identifier ENTITY_RADAR_CHANNEL_ID = Identifier.of(M0DevToolsClient.MOD_ID, "entity_radar/server_opt_in");

    private static boolean receiversRegistered = false;

    private RestrictedModuleOptInNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(InstantBreakOptInProbePayload.ID, InstantBreakOptInProbePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReachOptInProbePayload.ID, ReachOptInProbePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FreecamOptInProbePayload.ID, FreecamOptInProbePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EntityRadarOptInProbePayload.ID, EntityRadarOptInProbePayload.CODEC);
    }

    public static void registerReceivers() {
        if (receiversRegistered) {
            return;
        }
        receiversRegistered = true;

        // Receiver existence is enough for the client handshake check (ClientPlayNetworking.canSend).
        ServerPlayNetworking.registerGlobalReceiver(InstantBreakOptInProbePayload.ID, (payload, context) -> {
        });
        ServerPlayNetworking.registerGlobalReceiver(ReachOptInProbePayload.ID, (payload, context) -> {
        });
        ServerPlayNetworking.registerGlobalReceiver(FreecamOptInProbePayload.ID, (payload, context) -> {
        });
        ServerPlayNetworking.registerGlobalReceiver(EntityRadarOptInProbePayload.ID, (payload, context) -> {
        });
    }

    public record InstantBreakOptInProbePayload(boolean probe) implements CustomPayload {
        public static final CustomPayload.Id<InstantBreakOptInProbePayload> ID = new CustomPayload.Id<>(INSTANT_BREAK_CHANNEL_ID);
        public static final PacketCodec<RegistryByteBuf, InstantBreakOptInProbePayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new InstantBreakOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ReachOptInProbePayload(boolean probe) implements CustomPayload {
        public static final CustomPayload.Id<ReachOptInProbePayload> ID = new CustomPayload.Id<>(REACH_CHANNEL_ID);
        public static final PacketCodec<RegistryByteBuf, ReachOptInProbePayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new ReachOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record FreecamOptInProbePayload(boolean probe) implements CustomPayload {
        public static final CustomPayload.Id<FreecamOptInProbePayload> ID = new CustomPayload.Id<>(FREECAM_CHANNEL_ID);
        public static final PacketCodec<RegistryByteBuf, FreecamOptInProbePayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new FreecamOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record EntityRadarOptInProbePayload(boolean probe) implements CustomPayload {
        public static final CustomPayload.Id<EntityRadarOptInProbePayload> ID = new CustomPayload.Id<>(ENTITY_RADAR_CHANNEL_ID);
        public static final PacketCodec<RegistryByteBuf, EntityRadarOptInProbePayload> CODEC = PacketCodec.of(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new EntityRadarOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}

