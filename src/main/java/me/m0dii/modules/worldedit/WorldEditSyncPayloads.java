package me.m0dii.modules.worldedit;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class WorldEditSyncPayloads {
    private WorldEditSyncPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(EditRequestPayload.ID, EditRequestPayload.CODEC);
    }

    public enum Operation {
        SET,
        REPLACE,
        WALLS,
        FLOOR,
        ROOF,
        ENCLOSE,
        STACK,
        MOVE,
        CYL,
        HCYL,
        SPHERE,
        HSPHERE,
        UNDO,
        REDO
    }

    public record EditRequestPayload(Operation operation,
                                     BlockPos min,
                                     BlockPos max,
                                     String selectionShape,
                                     String primaryArg,
                                     String secondaryArg,
                                     int amount,
                                     String direction,
                                     boolean masked) implements CustomPayload {
        public static final CustomPayload.Id<EditRequestPayload> ID = new CustomPayload.Id<>(Identifier.of(M0DevToolsClient.MOD_ID, "worldedit/request"));
        public static final PacketCodec<RegistryByteBuf, EditRequestPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeVarInt(value.operation.ordinal());
                    buf.writeBlockPos(value.min);
                    buf.writeBlockPos(value.max);
                    buf.writeString(value.selectionShape);
                    buf.writeString(value.primaryArg);
                    buf.writeString(value.secondaryArg);
                    buf.writeVarInt(value.amount);
                    buf.writeString(value.direction);
                    buf.writeBoolean(value.masked);
                },
                buf -> {
                    Operation[] operations = Operation.values();
                    int ordinal = buf.readVarInt();
                    Operation operation = ordinal >= 0 && ordinal < operations.length ? operations[ordinal] : Operation.SET;
                    return new EditRequestPayload(
                            operation,
                            buf.readBlockPos(),
                            buf.readBlockPos(),
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readVarInt(),
                            buf.readString(),
                            buf.readBoolean()
                    );
                }
        );

        public EditRequestPayload(Operation operation,
                                  BlockPos min,
                                  BlockPos max,
                                  String selectionShape,
                                  String primaryArg,
                                  String secondaryArg,
                                  int amount,
                                  String direction,
                                  boolean masked) {
            this.operation = operation;
            this.min = min == null ? BlockPos.ORIGIN : min;
            this.max = max == null ? BlockPos.ORIGIN : max;
            this.selectionShape = selectionShape == null ? WorldEditRegionShape.BOX.name() : selectionShape;
            this.primaryArg = primaryArg == null ? "" : primaryArg;
            this.secondaryArg = secondaryArg == null ? "" : secondaryArg;
            this.amount = amount;
            this.direction = direction == null ? "" : direction;
            this.masked = masked;
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
