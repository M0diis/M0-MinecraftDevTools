package me.m0dii.modules.itemdata;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemDataSaveTest {
    private static CompoundTag item(String id, int count) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("count", count);
        return tag;
    }

    @Test
    void rejectsChangedCountReplacedItemAndChangedComponents() {
        CompoundTag expected = item("minecraft:diamond_sword", 1);
        assertTrue(ItemDataSyncServer.matchesExpected(expected, expected.copy()));
        assertFalse(ItemDataSyncServer.matchesExpected(expected, item("minecraft:diamond_sword", 2)));
        assertFalse(ItemDataSyncServer.matchesExpected(expected, item("minecraft:stick", 1)));
        CompoundTag damaged = expected.copy();
        CompoundTag components = new CompoundTag();
        components.putInt("minecraft:damage", 5);
        damaged.put("components", components);
        assertFalse(ItemDataSyncServer.matchesExpected(expected, damaged));
        assertFalse(ItemDataSyncServer.matchesExpected(null, expected));
        assertFalse(ItemDataSyncServer.matchesExpected(new CompoundTag(), expected));
    }

    @Test
    void savePayloadPreservesExpectedAndEditedSnapshots() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var payload = new ItemDataPayloads.ItemStackSavePayload(42L, 4, item("minecraft:stone", 3), item("minecraft:stone", 8));
            ItemDataPayloads.ItemStackSavePayload.CODEC.encode(buffer, payload);
            assertEquals(payload, ItemDataPayloads.ItemStackSavePayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void saveAcknowledgementPreservesFailureAndAuthoritativeData() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var result = new ItemDataPayloads.ItemStackSaveResultPayload(42L, false, "Item changed", item("minecraft:stone", 9));
            ItemDataPayloads.ItemStackSaveResultPayload.CODEC.encode(buffer, result);
            assertEquals(result, ItemDataPayloads.ItemStackSaveResultPayload.CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
