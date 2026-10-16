package me.m0dii.modules.mobdrops;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public final class MobDropTrackerPayloads {
    private MobDropTrackerPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.clientboundPlay().register(StatePayload.ID, StatePayload.CODEC);
    }

    public record ItemCountEntry(String itemId, long count) {
    }

    public record TrackerPayload(String name,
                                 String kind,
                                 String dimensionId,
                                 BlockPos anchor,
                                 BlockPos min,
                                 BlockPos max,
                                 long totalItems,
                                 long stackCount,
                                 long killCount,
                                 long dropsPerMinute,
                                 String lastMobType,
                                 List<ItemCountEntry> itemCounts) {
    }

    public record StatePayload(List<TrackerPayload> trackers,
                               String overlayMode,
                               List<String> overlayNames) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StatePayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "mobdrops/state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    writeTrackers(buf, value.trackers);
                    buf.writeUtf(value.overlayMode);
                    writeStrings(buf, value.overlayNames);
                },
                buf -> new StatePayload(readTrackers(buf), buf.readUtf(), readStrings(buf))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    private static void writeTrackers(RegistryFriendlyByteBuf buf, List<TrackerPayload> trackers) {
        buf.writeVarInt(trackers.size());
        for (TrackerPayload tracker : trackers) {
            buf.writeUtf(tracker.name);
            buf.writeUtf(tracker.kind);
            buf.writeUtf(tracker.dimensionId);
            buf.writeBlockPos(tracker.anchor);
            buf.writeBlockPos(tracker.min);
            buf.writeBlockPos(tracker.max);
            buf.writeLong(tracker.totalItems);
            buf.writeLong(tracker.stackCount);
            buf.writeLong(tracker.killCount);
            buf.writeLong(tracker.dropsPerMinute);
            buf.writeUtf(tracker.lastMobType);
            writeItemCounts(buf, tracker.itemCounts);
        }
    }

    private static List<TrackerPayload> readTrackers(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<TrackerPayload> trackers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            trackers.add(new TrackerPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readBlockPos(),
                    buf.readBlockPos(),
                    buf.readBlockPos(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readUtf(),
                    readItemCounts(buf)
            ));
        }
        return List.copyOf(trackers);
    }

    private static void writeItemCounts(RegistryFriendlyByteBuf buf, List<ItemCountEntry> itemCounts) {
        buf.writeVarInt(itemCounts.size());
        for (ItemCountEntry itemCount : itemCounts) {
            buf.writeUtf(itemCount.itemId);
            buf.writeLong(itemCount.count);
        }
    }

    private static List<ItemCountEntry> readItemCounts(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemCountEntry> itemCounts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            itemCounts.add(new ItemCountEntry(buf.readUtf(), buf.readLong()));
        }
        return List.copyOf(itemCounts);
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readUtf());
        }
        return List.copyOf(values);
    }
}
