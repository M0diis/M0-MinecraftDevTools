package me.m0dii.modules.chestfinder;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContainerMemoryTest {
    private static ContainerMemory sample() {
        ContainerMemory memory = new ContainerMemory();
        ContainerMemory.Entry a = new ContainerMemory.Entry("minecraft:overworld", 10L, "Chest", 1);
        a.add("minecraft:diamond", "Diamond", 5);
        a.add("minecraft:diamond", "Diamond", 7);
        a.add("minecraft:oak_log", "Oak Log", 64);
        ContainerMemory.Entry b = new ContainerMemory.Entry("minecraft:overworld", 20L, "Barrel", 2);
        b.add("minecraft:diamond_pickaxe", "Old Faithful", 1);
        b.add("minecraft:diamond", "Diamond", 30);
        ContainerMemory.Entry c = new ContainerMemory.Entry("minecraft:the_nether", 10L, "Chest", 3);
        c.add("minecraft:diamond", "Diamond", 99);
        memory.put(a);
        memory.put(b);
        memory.put(c);
        return memory;
    }

    @Test
    void searchSumsAndSortsPerDimension() {
        List<ContainerMemory.Match> matches = sample().search("diamond", "minecraft:overworld");
        assertEquals(2, matches.size());
        assertEquals(20L, matches.get(0).entry().pos);
        assertEquals(31L, matches.get(0).count());
        assertEquals(12L, matches.get(1).count());
        assertEquals(1, sample().search("diamond", "minecraft:the_nether").size());
    }

    @Test
    void matchesNamesAndSpaces() {
        ContainerMemory memory = sample();
        assertEquals(20L, memory.search("faithful", "minecraft:overworld").getFirst().entry().pos);
        assertEquals(10L, memory.search("oak log", "minecraft:overworld").getFirst().entry().pos);
        assertTrue(memory.search("  ", null).isEmpty());
        assertTrue(memory.search("emerald", null).isEmpty());
    }

    @Test
    void replaceRemoveAndRoundTrip() {
        ContainerMemory memory = sample();
        ContainerMemory.Entry emptied = new ContainerMemory.Entry("minecraft:overworld", 10L, "Chest", 9);
        memory.put(emptied);
        assertEquals(1, memory.search("diamond", "minecraft:overworld").size());
        assertEquals(3, memory.size());
        ContainerMemory copy = ContainerMemory.fromJson(memory.toJson());
        assertEquals(3, copy.size());
        assertEquals(31L, copy.search("diamond", "minecraft:overworld").getFirst().count());
        assertTrue(copy.remove("minecraft:overworld", 20L));
        assertFalse(copy.remove("minecraft:overworld", 20L));
        assertEquals(List.of("diamond"), copy.knownItems("minecraft:the_nether"));
        assertEquals(0, ContainerMemory.fromJson("").size());
    }

    @Test
    void totals() {
        List<ContainerMemory.Total> totals = sample().totals("minecraft:overworld", "");
        assertEquals("minecraft:oak_log", totals.get(0).itemId());
        assertEquals("minecraft:diamond", totals.get(1).itemId());
        assertEquals(42L, totals.get(1).count());
        assertEquals(2L, totals.get(1).containers());
        assertEquals(2, sample().totals("minecraft:overworld", "diamond").size());
    }
}
