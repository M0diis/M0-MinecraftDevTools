package me.m0dii.modules.optin;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class RestrictedModuleOptInNetworking {
    public static final Identifier INSTANT_BREAK_CHANNEL_ID = Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "instant_break/server_opt_in");
    public static final Identifier REACH_CHANNEL_ID = Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "reach/server_opt_in");
    public static final Identifier FREECAM_CHANNEL_ID = Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "freecam/server_opt_in");
    public static final Identifier ENTITY_RADAR_CHANNEL_ID = Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "entity_radar/server_opt_in");

    private static boolean receiversRegistered = false;

    private RestrictedModuleOptInNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(InstantBreakOptInProbePayload.ID, InstantBreakOptInProbePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReachOptInProbePayload.ID, ReachOptInProbePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FreecamOptInProbePayload.ID, FreecamOptInProbePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EntityRadarOptInProbePayload.ID, EntityRadarOptInProbePayload.CODEC);
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

    public record InstantBreakOptInProbePayload(boolean probe) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<InstantBreakOptInProbePayload> ID = new CustomPacketPayload.Type<>(INSTANT_BREAK_CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, InstantBreakOptInProbePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new InstantBreakOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ReachOptInProbePayload(boolean probe) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReachOptInProbePayload> ID = new CustomPacketPayload.Type<>(REACH_CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, ReachOptInProbePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new ReachOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record FreecamOptInProbePayload(boolean probe) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FreecamOptInProbePayload> ID = new CustomPacketPayload.Type<>(FREECAM_CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, FreecamOptInProbePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new FreecamOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record EntityRadarOptInProbePayload(boolean probe) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EntityRadarOptInProbePayload> ID = new CustomPacketPayload.Type<>(ENTITY_RADAR_CHANNEL_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, EntityRadarOptInProbePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> buf.writeBoolean(value.probe),
                buf -> new EntityRadarOptInProbePayload(buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}

