package me.m0dii.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PickBlockHotbarSlotResolverTest {

    @Test
    void prefersExistingMatchingHotbarStack() {
        List<ItemStack> hotbar = emptyHotbar();
        hotbar.set(4, new ItemStack(Items.STONE));

        int slot = PickBlockHotbarSlotResolver.resolveTargetSlot(hotbar, 1, new ItemStack(Items.STONE));

        assertEquals(4, slot);
    }

    @Test
    void usesFirstEmptyHotbarSlotBeforeOverwritingSelected() {
        List<ItemStack> hotbar = emptyHotbar();
        hotbar.set(0, new ItemStack(Items.DIRT));
        hotbar.set(1, new ItemStack(Items.COBBLESTONE));

        int slot = PickBlockHotbarSlotResolver.resolveTargetSlot(hotbar, 1, new ItemStack(Items.STONE));

        assertEquals(2, slot);
    }

    @Test
    void fallsBackToSelectedSlotWhenHotbarIsFullAndNoMatchExists() {
        List<ItemStack> hotbar = new ArrayList<>(List.of(
                new ItemStack(Items.DIRT),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.SAND),
                new ItemStack(Items.GRAVEL),
                new ItemStack(Items.GLASS),
                new ItemStack(Items.BRICKS),
                new ItemStack(Items.TERRACOTTA),
                new ItemStack(Items.NETHERRACK)
        ));

        int slot = PickBlockHotbarSlotResolver.resolveTargetSlot(hotbar, 6, new ItemStack(Items.STONE));

        assertEquals(6, slot);
    }

    private static List<ItemStack> emptyHotbar() {
        List<ItemStack> hotbar = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            hotbar.add(ItemStack.EMPTY);
        }
        return hotbar;
    }
}
