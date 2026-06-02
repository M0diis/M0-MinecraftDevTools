package me.m0dii.modules.getdata;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class GetDataSyncPayloads {
    private GetDataSyncPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(BlockNbtRequestPayload.ID, BlockNbtRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BlockNbtResponsePayload.ID, BlockNbtResponsePayload.CODEC);
    }

    public record BlockNbtRequestPayload(long requestId, BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<BlockNbtRequestPayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "getdata/request_block"));
        public static final PacketCodec<RegistryByteBuf, BlockNbtRequestPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeBlockPos(value.pos);
                },
                buf -> new BlockNbtRequestPayload(buf.readLong(), buf.readBlockPos())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BlockNbtResponsePayload(long requestId, BlockPos pos, NbtCompound nbt) implements CustomPayload {
        public static final CustomPayload.Id<BlockNbtResponsePayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "getdata/response_block"));
        public static final PacketCodec<RegistryByteBuf, BlockNbtResponsePayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeBlockPos(value.pos);
                    buf.writeNbt(value.nbt);
                },
                buf -> new BlockNbtResponsePayload(buf.readLong(), buf.readBlockPos(), buf.readNbt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}

