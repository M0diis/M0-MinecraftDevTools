package me.m0dii.modules.itemdata;

import me.m0dii.M0DevToolsClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ItemDataPayloads {
    private ItemDataPayloads() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.serverboundPlay().register(ItemStackRequestPayload.ID, ItemStackRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ItemStackSavePayload.ID, ItemStackSavePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ItemStackResponsePayload.ID, ItemStackResponsePayload.CODEC);
    }

    public record ItemStackRequestPayload(long requestId, int slotIndex) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ItemStackRequestPayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "itemdata/request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackRequestPayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeInt(value.slotIndex);
                },
                buf -> new ItemStackRequestPayload(buf.readLong(), buf.readInt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ItemStackResponsePayload(long requestId, int slotIndex, boolean found, CompoundTag itemData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ItemStackResponsePayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "itemdata/response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackResponsePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeLong(value.requestId);
                    buf.writeInt(value.slotIndex);
                    buf.writeBoolean(value.found);
                    buf.writeNbt(value.itemData);
                },
                buf -> new ItemStackResponsePayload(buf.readLong(), buf.readInt(), buf.readBoolean(), buf.readNbt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record ItemStackSavePayload(int slotIndex, CompoundTag itemData) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ItemStackSavePayload> ID = new CustomPacketPayload.Type<>(
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "itemdata/save"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackSavePayload> CODEC = StreamCodec.ofMember(
                (value, buf) -> {
                    buf.writeInt(value.slotIndex);
                    buf.writeNbt(value.itemData);
                },
                buf -> new ItemStackSavePayload(buf.readInt(), buf.readNbt())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
