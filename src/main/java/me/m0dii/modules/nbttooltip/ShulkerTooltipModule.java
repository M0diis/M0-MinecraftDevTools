package me.m0dii.modules.nbttooltip;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.ArrayList;
import java.util.List;

public class ShulkerTooltipModule extends Module {

    public static final ShulkerTooltipModule INSTANCE = new ShulkerTooltipModule();

    private ShulkerTooltipModule() {
        super("shulker_tooltip", "Shulker Preview Tooltip", true);
    }

    @Override
    public void register() {
        ClientTooltipComponentCallback.EVENT.register(data -> {
            if (!isEnabled() || !(data instanceof ShulkerPreviewTooltipData(List<ItemStack> stacks))) {
                return null;
            }
            return new ShulkerPreviewTooltipComponent(stacks);
        });
    }

    public static boolean canPreview(ItemStack stack) {
        return stack != null
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public static List<ItemStack> readContainerStacks(ItemStack stack) {
        List<ItemStack> out = new ArrayList<>();
        try {
            ItemContainerContents container = stack.getComponents().get(DataComponents.CONTAINER);
            if (container != null) {
                container.nonEmptyItemCopyStream().forEach(out::add);
            }
        } catch (Throwable ignored) {
            // Keep tooltip path safe if a modded container component misbehaves.
        }
        return out;
    }
}

