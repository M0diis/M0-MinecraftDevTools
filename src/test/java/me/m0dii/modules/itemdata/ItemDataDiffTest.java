package me.m0dii.modules.itemdata;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDataDiffTest {
    @Test
    void listsAddedRemovedAndChanged() {
        Map<String, String> before = Map.of(
                "id", "\"minecraft:diamond_sword\"",
                "count", "1",
                "components/minecraft:damage", "5");
        Map<String, String> after = Map.of(
                "id", "\"minecraft:diamond_sword\"",
                "count", "2",
                "components/minecraft:custom_name", "\"Blade\"");
        assertEquals(List.of(
                "+ components/minecraft:custom_name = \"Blade\"",
                "- components/minecraft:damage",
                "~ count: 1 → 2"), ItemDataDiff.diff(before, after));
    }

    @Test
    void noChangesMeansEmpty() {
        assertTrue(ItemDataDiff.diff(Map.of("a", "1"), Map.of("a", "1")).isEmpty());
    }

    @Test
    void longValuesAreShortened() {
        String longValue = "x".repeat(200);
        String line = ItemDataDiff.diff(Map.of(), Map.of("k", longValue)).getFirst();
        assertTrue(line.length() < 120 && line.endsWith("…"), line);
    }
}
