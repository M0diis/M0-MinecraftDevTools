package me.m0dii.modules.mobai;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class MobAiDebugPayloads {
    private MobAiDebugPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(InspectPayload.ID, InspectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PathPreviewPayload.ID, PathPreviewPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClearPayload.ID, ClearPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TrackerConfigPayload.ID, TrackerConfigPayload.CODEC);
    }

    public record DebugLine(String text, int color) {
    }

    public record InspectPayload(int entityId, List<DebugLine> lines) implements CustomPayload {
        public static final CustomPayload.Id<InspectPayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "mobai/inspect"));
        public static final PacketCodec<RegistryByteBuf, InspectPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeVarInt(value.entityId);
                    writeLines(buf, value.lines);
                },
                buf -> new InspectPayload(buf.readVarInt(), readLines(buf))
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PathPreviewPayload(int entityId,
                                     BlockPos target,
                                     boolean pathFound,
                                     boolean reachesTarget,
                                     int currentNodeIndex,
                                     int manhattanDistance,
                                     List<BlockPos> nodes,
                                     List<BlockPos> openNodes,
                                     List<BlockPos> closedNodes,
                                     List<DebugLine> lines) implements CustomPayload {
        public static final CustomPayload.Id<PathPreviewPayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "mobai/path_preview"));
        public static final PacketCodec<RegistryByteBuf, PathPreviewPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeVarInt(value.entityId);
                    buf.writeBlockPos(value.target);
                    buf.writeBoolean(value.pathFound);
                    buf.writeBoolean(value.reachesTarget);
                    buf.writeVarInt(value.currentNodeIndex);
                    buf.writeVarInt(value.manhattanDistance);
                    writeBlockPositions(buf, value.nodes);
                    writeBlockPositions(buf, value.openNodes);
                    writeBlockPositions(buf, value.closedNodes);
                    writeLines(buf, value.lines);
                },
                buf -> new PathPreviewPayload(
                        buf.readVarInt(),
                        buf.readBlockPos(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        readBlockPositions(buf),
                        readBlockPositions(buf),
                        readBlockPositions(buf),
                        readLines(buf))
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClearPayload(boolean clearInspect, boolean clearPathPreview) implements CustomPayload {
        public static final CustomPayload.Id<ClearPayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "mobai/clear"));
        public static final PacketCodec<RegistryByteBuf, ClearPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeBoolean(value.clearInspect);
                    buf.writeBoolean(value.clearPathPreview);
                },
                buf -> new ClearPayload(buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record TrackerConfigPayload(List<String> enabledDisplays,
                                       boolean showBoxes,
                                       int alpha,
                                       int radius,
                                       String hostileFocus) implements CustomPayload {
        public static final CustomPayload.Id<TrackerConfigPayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "mobai/tracker_config"));
        public static final PacketCodec<RegistryByteBuf, TrackerConfigPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    writeStrings(buf, value.enabledDisplays);
                    buf.writeBoolean(value.showBoxes);
                    buf.writeVarInt(value.alpha);
                    buf.writeVarInt(value.radius);
                    buf.writeString(value.hostileFocus);
                },
                buf -> new TrackerConfigPayload(
                        readStrings(buf),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readString())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static void writeLines(RegistryByteBuf buf, List<DebugLine> lines) {
        buf.writeVarInt(lines.size());
        for (DebugLine line : lines) {
            buf.writeString(line.text);
            buf.writeInt(line.color);
        }
    }

    private static List<DebugLine> readLines(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<DebugLine> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(new DebugLine(buf.readString(), buf.readInt()));
        }
        return List.copyOf(lines);
    }

    private static void writeBlockPositions(RegistryByteBuf buf, List<BlockPos> positions) {
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    private static List<BlockPos> readBlockPositions(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return List.copyOf(positions);
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
