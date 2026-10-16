package me.m0dii.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NbtEditorUtilsTest {
    @Test
    void resolvesCompoundAndListPaths() {
        CompoundTag root = new CompoundTag();
        root.putFloat("Health", 20.0f);

        CompoundTag offer = new CompoundTag();
        offer.putString("id", "minecraft:emerald");
        ListTag recipes = new ListTag();
        recipes.add(offer);

        CompoundTag offers = new CompoundTag();
        offers.put("Recipes", recipes);
        root.put("Offers", offers);

        assertEquals("compound (2 keys)", NbtEditorUtils.summary(root));
        assertEquals("20.0f", NbtEditorUtils.resolvePath(root, List.of("Health")).toString());
        assertEquals("\"minecraft:emerald\"", NbtEditorUtils.resolvePath(root, List.of("Offers", "Recipes", "0", "id")).toString());
        assertEquals("Offers.Recipes[0].id", NbtEditorUtils.formatPath(List.of("Offers", "Recipes", "0", "id")));
    }

    @Test
    void updatesAndRemovesChildren() {
        CompoundTag root = new CompoundTag();
        root.put("foo", StringTag.valueOf("bar"));

        ListTag list = new ListTag();
        list.add(IntTag.valueOf(1));
        list.add(IntTag.valueOf(2));
        root.put("list", list);

        NbtEditorUtils.setChild(root, "foo", StringTag.valueOf("baz"));
        assertEquals("\"baz\"", root.get("foo").toString());

        NbtEditorUtils.setChild(list, "1", IntTag.valueOf(5));
        assertEquals("5", list.get(1).toString());

        NbtEditorUtils.removeChild(root, "foo");
        assertFalse(root.contains("foo"));

        NbtEditorUtils.removeChild(list, "0");
        assertEquals(1, list.size());
        assertEquals("5", list.getFirst().toString());
    }
}
