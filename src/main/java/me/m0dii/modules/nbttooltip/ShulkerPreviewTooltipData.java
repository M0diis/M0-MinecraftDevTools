package me.m0dii.modules.nbttooltip;

import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;

import java.util.ArrayList;
import java.util.List;

public record ShulkerPreviewTooltipData(List<ItemStack> stacks) implements TooltipData {
    public ShulkerPreviewTooltipData {
        stacks = stacks == null ? List.of() : new ArrayList<>(stacks);
    }
}

