package me.m0dii.modules.nbtcomplete;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class NbtCompletionTest {
    private static final Function<String, List<String>> REGISTRY = name -> switch (name) {
        case "mob_effect" -> List.of("minecraft:speed", "minecraft:slowness", "minecraft:strength");
        case "item" -> List.of("minecraft:diamond_sword", "minecraft:stone");
        case "entity_type" -> List.of("minecraft:zombie", "minecraft:pig");
        default -> List.of();
    };

    @Test
    void suggestsRootKeysAndSkipsUsedOnes() {
        NbtCompletion.Result r = NbtCompletion.compound("{NoAI:1b,No", "living", REGISTRY);
        assertEquals(9, r.start());
        assertTrue(r.suggestions().contains("NoGravity:"));
        assertFalse(r.suggestions().contains("NoAI:"));
        assertEquals("NoGravity:", r.suggestions().getFirst());
    }

    @Test
    void emptyCompoundListsEverythingAndTypeSpecificKeys() {
        NbtCompletion.Result r = NbtCompletion.compound("{", NbtSchema.entityRoot("minecraft:creeper"), REGISTRY);
        assertEquals(1, r.start());
        assertTrue(r.suggestions().contains("Fuse:"));
        assertTrue(r.suggestions().contains("Health:"));
        assertTrue(r.suggestions().contains("Glowing:"));
        NbtCompletion.Result item = NbtCompletion.compound("{", NbtSchema.entityRoot("item"), REGISTRY);
        assertTrue(item.suggestions().contains("Item:"));
        assertFalse(item.suggestions().contains("Health:"));
    }

    @Test
    void valuesByKind() {
        assertEquals(List.of("1b", "0b"), NbtCompletion.compound("{Silent:", "living", REGISTRY).suggestions());
        NbtCompletion.Result floats = NbtCompletion.compound("{Health: ", "living", REGISTRY);
        assertEquals(9, floats.start());
        assertEquals("0.0f", floats.suggestions().getFirst());
        assertEquals(List.of("[{"), NbtCompletion.compound("{active_effects:", "living", REGISTRY).suggestions());
    }

    @Test
    void nestedListsAndCompounds() {
        NbtCompletion.Result r = NbtCompletion.compound("{active_effects:[{id:\"minecraft:speed\",am", "living", REGISTRY);
        assertEquals(List.of("amplifier:", "ambient:"), r.suggestions());
        NbtCompletion.Result ids = NbtCompletion.compound("{active_effects:[{id:\"minecraft:s", "living", REGISTRY);
        assertEquals(List.of("\"minecraft:speed\"", "\"minecraft:slowness\"", "\"minecraft:strength\""), ids.suggestions());
        assertEquals(21, ids.start());
        NbtCompletion.Result eq = NbtCompletion.compound("{equipment:{mainhand:{id:\"minecraft:stone\",c", "living", REGISTRY);
        assertEquals(List.of("count:", "components:"), eq.suggestions());
        NbtCompletion.Result list = NbtCompletion.compound("{active_effects:[{id:\"a\"},", "living", REGISTRY);
        assertEquals(List.of("{"), list.suggestions());
        NbtCompletion.Result passenger = NbtCompletion.compound("{Passengers:[{i", "living", REGISTRY);
        assertTrue(passenger.suggestions().contains("id:"));
    }

    @Test
    void ignoresSeparatorsInsideStrings() {
        NbtCompletion.Result r = NbtCompletion.compound("{CustomName:\"a,b:{c\",Sil", "living", REGISTRY);
        assertEquals(List.of("Silent:"), r.suggestions());
        assertTrue(NbtCompletion.compound("{CustomName:\"open", "living", REGISTRY).isEmpty());
        assertTrue(NbtCompletion.compound("{a:1}", "living", REGISTRY).isEmpty());
    }

    @Test
    void blockRoots() {
        assertEquals("container", NbtSchema.blockRoot("minecraft:chest"));
        assertEquals("block:sign", NbtSchema.blockRoot("oak_hanging_sign"));
        NbtCompletion.Result r = NbtCompletion.compound("{Items:[{S", "container", REGISTRY);
        assertEquals("Slot:", r.suggestions().getFirst());
    }

    @Test
    void paths() {
        NbtCompletion.Result root = NbtCompletion.path("equ", "living");
        assertEquals(0, root.start());
        assertEquals("equipment", root.suggestions().getFirst());
        NbtCompletion.Result nested = NbtCompletion.path("equipment.main", "living");
        assertEquals(10, nested.start());
        assertEquals(List.of("mainhand"), nested.suggestions());
        NbtCompletion.Result deep = NbtCompletion.path("equipment.mainhand.", "living");
        assertTrue(deep.suggestions().contains("id"));
        NbtCompletion.Result list = NbtCompletion.path("active_effects[0].dur", "living");
        assertEquals(List.of("duration"), list.suggestions());
        assertTrue(NbtCompletion.path("Health.x", "living").isEmpty());
        assertTrue(NbtCompletion.path("Tags", "living").suggestions().contains("Tags["));
    }

    @Test
    void componentValues() {
        NbtCompletion.Result r = NbtCompletion.componentValue("diamond_sword[minecraft:rarity=\"e");
        assertEquals(31, r.start());
        assertEquals("\"epic\"", r.suggestions().getFirst());
        NbtCompletion.Result glint = NbtCompletion.componentValue("stone[custom_name=\"x\",enchantment_glint_override=");
        assertEquals(List.of("true", "false"), glint.suggestions());
        assertTrue(NbtCompletion.componentValue("stone[custom_na").isEmpty());
        assertTrue(NbtCompletion.componentValue("stone[enchantments={").isEmpty());
        assertTrue(NbtCompletion.componentValue("stone").isEmpty());
    }
}
