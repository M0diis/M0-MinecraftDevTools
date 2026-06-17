package me.m0dii.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PickBlockHotbarSlotResolverTest {

    @Test
    void prefersExistingMatchingHotbarStack() {
        List<String> hotbar = emptyHotbar();
        hotbar.set(4, "stone");

        int slot = PickBlockHotbarSlotResolver.resolveTargetSlot(hotbar, 1, "stone", PickBlockHotbarSlotResolverTest::isEmpty, Objects::equals);

        assertEquals(4, slot);
    }

    @Test
    void usesFirstEmptyHotbarSlotBeforeOverwritingSelected() {
        List<String> hotbar = emptyHotbar();
        hotbar.set(0, "dirt");
        hotbar.set(1, "cobblestone");

        int slot = PickBlockHotbarSlotResolver.resolveTargetSlot(hotbar, 1, "stone", PickBlockHotbarSlotResolverTest::isEmpty, Objects::equals);

        assertEquals(2, slot);
    }

    @Test
    void fallsBackToSelectedSlotWhenHotbarIsFullAndNoMatchExists() {
        List<String> hotbar = new ArrayList<>(List.of(
                "dirt",
                "cobblestone",
                "oak_planks",
                "sand",
                "gravel",
                "glass",
                "bricks",
                "terracotta",
                "netherrack"
        ));

        int slot = PickBlockHotbarSlotResolver.resolveTargetSlot(hotbar, 6, "stone", PickBlockHotbarSlotResolverTest::isEmpty, Objects::equals);

        assertEquals(6, slot);
    }

    private static List<String> emptyHotbar() {
        List<String> hotbar = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            hotbar.add(null);
        }
        return hotbar;
    }

    private static boolean isEmpty(String stack) {
        return stack == null;
    }
}
