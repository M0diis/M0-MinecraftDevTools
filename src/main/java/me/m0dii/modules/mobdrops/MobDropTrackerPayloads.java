package me.m0dii.modules.mobdrops;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class MobDropTrackerPayloads {
    private MobDropTrackerPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
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
                               List<String> overlayNames) implements CustomPayload {
        public static final CustomPayload.Id<StatePayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "mobdrops/state"));
        public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    writeTrackers(buf, value.trackers);
                    buf.writeString(value.overlayMode);
                    writeStrings(buf, value.overlayNames);
                },
                buf -> new StatePayload(readTrackers(buf), buf.readString(), readStrings(buf))
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static void writeTrackers(RegistryByteBuf buf, List<TrackerPayload> trackers) {
        buf.writeVarInt(trackers.size());
        for (TrackerPayload tracker : trackers) {
            buf.writeString(tracker.name);
            buf.writeString(tracker.kind);
            buf.writeString(tracker.dimensionId);
            buf.writeBlockPos(tracker.anchor);
            buf.writeBlockPos(tracker.min);
            buf.writeBlockPos(tracker.max);
            buf.writeLong(tracker.totalItems);
            buf.writeLong(tracker.stackCount);
            buf.writeLong(tracker.killCount);
            buf.writeLong(tracker.dropsPerMinute);
            buf.writeString(tracker.lastMobType);
            writeItemCounts(buf, tracker.itemCounts);
        }
    }

    private static List<TrackerPayload> readTrackers(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<TrackerPayload> trackers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            trackers.add(new TrackerPayload(
                    buf.readString(),
                    buf.readString(),
                    buf.readString(),
                    buf.readBlockPos(),
                    buf.readBlockPos(),
                    buf.readBlockPos(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readString(),
                    readItemCounts(buf)
            ));
        }
        return List.copyOf(trackers);
    }

    private static void writeItemCounts(RegistryByteBuf buf, List<ItemCountEntry> itemCounts) {
        buf.writeVarInt(itemCounts.size());
        for (ItemCountEntry itemCount : itemCounts) {
            buf.writeString(itemCount.itemId);
            buf.writeLong(itemCount.count);
        }
    }

    private static List<ItemCountEntry> readItemCounts(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<ItemCountEntry> itemCounts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            itemCounts.add(new ItemCountEntry(buf.readString(), buf.readLong()));
        }
        return List.copyOf(itemCounts);
    }

    private static void writeStrings(RegistryByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeString(value);
        }
    }

    private static List<String> readStrings(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readString());
        }
        return List.copyOf(values);
    }
}
