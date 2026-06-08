package me.m0dii.utils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NbtEditorUtilsTest {
    @Test
    void resolvesCompoundAndListPaths() {
        NbtCompound root = new NbtCompound();
        root.putFloat("Health", 20.0f);

        NbtCompound offer = new NbtCompound();
        offer.putString("id", "minecraft:emerald");
        NbtList recipes = new NbtList();
        recipes.add(offer);

        NbtCompound offers = new NbtCompound();
        offers.put("Recipes", recipes);
        root.put("Offers", offers);

        assertEquals("compound (2 keys)", NbtEditorUtils.summary(root));
        assertEquals("20.0f", NbtEditorUtils.resolvePath(root, List.of("Health")).toString());
        assertEquals("\"minecraft:emerald\"", NbtEditorUtils.resolvePath(root, List.of("Offers", "Recipes", "0", "id")).toString());
        assertEquals("Offers.Recipes[0].id", NbtEditorUtils.formatPath(List.of("Offers", "Recipes", "0", "id")));
    }

    @Test
    void updatesAndRemovesChildren() {
        NbtCompound root = new NbtCompound();
        root.put("foo", NbtString.of("bar"));

        NbtList list = new NbtList();
        list.add(NbtInt.of(1));
        list.add(NbtInt.of(2));
        root.put("list", list);

        NbtEditorUtils.setChild(root, "foo", NbtString.of("baz"));
        assertEquals("\"baz\"", root.get("foo").toString());

        NbtEditorUtils.setChild(list, "1", NbtInt.of(5));
        assertEquals("5", list.get(1).toString());

        NbtEditorUtils.removeChild(root, "foo");
        assertFalse(root.contains("foo"));

        NbtEditorUtils.removeChild(list, "0");
        assertEquals(1, list.size());
        assertEquals("5", list.getFirst().toString());
    }
}
