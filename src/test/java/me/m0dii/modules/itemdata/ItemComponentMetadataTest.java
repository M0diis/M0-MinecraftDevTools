package me.m0dii.modules.itemdata;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemComponentMetadataTest {

    @Test
    void knownComponentHasDescriptionTemplateAndExample() {
        assertEquals("Custom Name", ItemComponentMetadata.label("minecraft:custom_name"));
        assertEquals("Text", ItemComponentMetadata.typeHint("minecraft:custom_name"));
        assertFalse(ItemComponentMetadata.description("minecraft:custom_name").isBlank());
        assertTrue(ItemComponentMetadata.starterTemplate("minecraft:custom_name", null).contains("Renamed Item"));
        assertTrue(ItemComponentMetadata.example("minecraft:custom_name", null).contains("OP Sword"));
    }

    @Test
    void countHelpersRoundTripAndValidate() throws CommandSyntaxException {
        NbtCompound itemData = new NbtCompound();
        assertEquals(1, ItemDataCodec.readCount(itemData));

        ItemDataCodec.applyCount(itemData, "16");
        assertEquals(16, ItemDataCodec.readCount(itemData));

        assertThrows(CommandSyntaxException.class, () -> ItemDataCodec.applyCount(itemData, "0"));
        assertThrows(CommandSyntaxException.class, () -> ItemDataCodec.applyCount(itemData, "abc"));
    }
}
