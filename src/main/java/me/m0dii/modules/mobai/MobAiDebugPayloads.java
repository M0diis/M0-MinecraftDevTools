package me.m0dii.modules.mobai;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;

public final class MobAiDebugPayloads {
    private MobAiDebugPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.clientboundPlay().register(InspectPayload.ID, InspectPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PathPreviewPayload.ID, PathPreviewPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClearPayload.ID, ClearPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrackerConfigPayload.ID, TrackerConfigPayload.CODEC);
    }

    public record DebugLine(String text, int color) {
    }

    public record InspectPayload(int entityId, List<DebugLine> lines) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<InspectPayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "mobai/inspect"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InspectPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeVarInt(value.entityId);
                    writeLines(buf, value.lines);
                },
                buf -> new InspectPayload(buf.readVarInt(), readLines(buf))
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
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
                                     List<DebugLine> lines) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PathPreviewPayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "mobai/path_preview"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PathPreviewPayload> CODEC = StreamCodec.ofMember(
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
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ClearPayload(boolean clearInspect, boolean clearPathPreview) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ClearPayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "mobai/clear"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClearPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeBoolean(value.clearInspect);
                    buf.writeBoolean(value.clearPathPreview);
                },
                buf -> new ClearPayload(buf.readBoolean(), buf.readBoolean())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record TrackerConfigPayload(List<String> enabledDisplays,
                                       boolean showBoxes,
                                       int alpha,
                                       int radius,
                                       String hostileFocus) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TrackerConfigPayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "mobai/tracker_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TrackerConfigPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    writeStrings(buf, value.enabledDisplays);
                    buf.writeBoolean(value.showBoxes);
                    buf.writeVarInt(value.alpha);
                    buf.writeVarInt(value.radius);
                    buf.writeUtf(value.hostileFocus);
                },
                buf -> new TrackerConfigPayload(
                        readStrings(buf),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readUtf())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    private static void writeLines(RegistryFriendlyByteBuf buf, List<DebugLine> lines) {
        buf.writeVarInt(lines.size());
        for (DebugLine line : lines) {
            buf.writeUtf(line.text);
            buf.writeInt(line.color);
        }
    }

    private static List<DebugLine> readLines(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<DebugLine> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(new DebugLine(buf.readUtf(), buf.readInt()));
        }
        return List.copyOf(lines);
    }

    private static void writeBlockPositions(RegistryFriendlyByteBuf buf, List<BlockPos> positions) {
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    private static List<BlockPos> readBlockPositions(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return List.copyOf(positions);
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
