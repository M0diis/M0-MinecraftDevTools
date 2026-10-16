package me.m0dii.modules.worldedit;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class WorldEditSyncPayloads {
    private WorldEditSyncPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(EditRequestPayload.ID, EditRequestPayload.CODEC);
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
                                     boolean masked) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EditRequestPayload> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "worldedit/request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, EditRequestPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeVarInt(value.operation.ordinal());
                    buf.writeBlockPos(value.min);
                    buf.writeBlockPos(value.max);
                    buf.writeUtf(value.selectionShape);
                    buf.writeUtf(value.primaryArg);
                    buf.writeUtf(value.secondaryArg);
                    buf.writeVarInt(value.amount);
                    buf.writeUtf(value.direction);
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
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readVarInt(),
                            buf.readUtf(),
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
            this.min = min == null ? BlockPos.ZERO : min;
            this.max = max == null ? BlockPos.ZERO : max;
            this.selectionShape = selectionShape == null ? WorldEditRegionShape.BOX.name() : selectionShape;
            this.primaryArg = primaryArg == null ? "" : primaryArg;
            this.secondaryArg = secondaryArg == null ? "" : secondaryArg;
            this.amount = amount;
            this.direction = direction == null ? "" : direction;
            this.masked = masked;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
