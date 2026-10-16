package me.m0dii.modules.nbttooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ShulkerPreviewTooltipData(List<ItemStack> stacks) implements TooltipComponent {
    public ShulkerPreviewTooltipData {
        stacks = stacks == null ? List.of() : new ArrayList<>(stacks);
    }
}

