package me.m0dii.modules.getdata;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class GetDataSyncPayloads {
    private GetDataSyncPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(BlockNbtRequestPayload.ID, BlockNbtRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BlockNbtResponsePayload.ID, BlockNbtResponsePayload.CODEC);
    }

    public record BlockNbtRequestPayload(long requestId, BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlockNbtRequestPayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "getdata/request_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockNbtRequestPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeBlockPos(value.pos);
                },
                buf -> new BlockNbtRequestPayload(buf.readLong(), buf.readBlockPos())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record BlockNbtResponsePayload(long requestId, BlockPos pos, CompoundTag nbt) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlockNbtResponsePayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "getdata/response_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BlockNbtResponsePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeBlockPos(value.pos);
                    buf.writeNbt(value.nbt);
                },
                buf -> new BlockNbtResponsePayload(buf.readLong(), buf.readBlockPos(), buf.readNbt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}

