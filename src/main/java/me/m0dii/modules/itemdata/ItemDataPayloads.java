package me.m0dii.modules.itemdata;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ItemDataPayloads {
    private ItemDataPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playC2S().register(ItemStackRequestPayload.ID, ItemStackRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ItemStackSavePayload.ID, ItemStackSavePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ItemStackResponsePayload.ID, ItemStackResponsePayload.CODEC);
    }

    public record ItemStackRequestPayload(long requestId, int slotIndex) implements CustomPayload {
        public static final CustomPayload.Id<ItemStackRequestPayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "itemdata/request"));
        public static final PacketCodec<RegistryByteBuf, ItemStackRequestPayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeInt(value.slotIndex);
                },
                buf -> new ItemStackRequestPayload(buf.readLong(), buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ItemStackResponsePayload(long requestId, int slotIndex, boolean found, NbtCompound itemData) implements CustomPayload {
        public static final CustomPayload.Id<ItemStackResponsePayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "itemdata/response"));
        public static final PacketCodec<RegistryByteBuf, ItemStackResponsePayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeInt(value.slotIndex);
                    buf.writeBoolean(value.found);
                    buf.writeNbt(value.itemData);
                },
                buf -> new ItemStackResponsePayload(buf.readLong(), buf.readInt(), buf.readBoolean(), buf.readNbt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ItemStackSavePayload(int slotIndex, NbtCompound itemData) implements CustomPayload {
        public static final CustomPayload.Id<ItemStackSavePayload> ID = new CustomPayload.Id<>(
                Identifier.of(M0DevToolsClient.MOD_ID, "itemdata/save"));
        public static final PacketCodec<RegistryByteBuf, ItemStackSavePayload> CODEC = PacketCodec.of(
                (value, buf) -> {
                    buf.writeInt(value.slotIndex);
                    buf.writeNbt(value.itemData);
                },
                buf -> new ItemStackSavePayload(buf.readInt(), buf.readNbt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
