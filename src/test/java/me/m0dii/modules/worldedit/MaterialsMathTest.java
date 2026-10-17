package me.m0dii.modules.worldedit;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaterialsMathTest {
    @Test
    void stackText() {
        assertEquals("0", MaterialsMath.stacks(0));
        assertEquals("63", MaterialsMath.stacks(63));
        assertEquals("1 stack", MaterialsMath.stacks(64));
        assertEquals("3 stacks + 12", MaterialsMath.stacks(3 * 64 + 12));
        assertEquals("1 shulker", MaterialsMath.stacks(1728));
        assertEquals("2 shulkers + 1 stack + 1", MaterialsMath.stacks(2 * 1728 + 65));
    }

    @Test
    void sortsByCountThenNameAndWritesCsv() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("stone", 10L);
        counts.put("dirt", 200L);
        counts.put("andesite", 10L);
        List<Map.Entry<String, Long>> sorted = MaterialsMath.sorted(counts, Function.identity());
        assertEquals("dirt", sorted.get(0).getKey());
        assertEquals("andesite", sorted.get(1).getKey());
        assertEquals("stone", sorted.get(2).getKey());
        String csv = MaterialsMath.csv(sorted, k -> "minecraft:" + k, k -> k.equals("dirt") ? "Dirt \"x\"" : k);
        assertEquals("block,name,count,stacks\n"
                + "minecraft:dirt,\"Dirt \"\"x\"\"\",200,3.13\n"
                + "minecraft:andesite,\"andesite\",10,0.16\n"
                + "minecraft:stone,\"stone\",10,0.16\n", csv);
    }
}
