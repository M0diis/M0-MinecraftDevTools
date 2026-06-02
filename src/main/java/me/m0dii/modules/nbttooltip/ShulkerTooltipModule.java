package me.m0dii.modules.nbttooltip;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShulkerTooltipModule extends Module {

    public static final ShulkerTooltipModule INSTANCE = new ShulkerTooltipModule();

    private ShulkerTooltipModule() {
        super("shulker_tooltip", "Shulker Preview Tooltip", true);
    }

    @Override
    public void register() {
        TooltipComponentCallback.EVENT.register(data -> {
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
            Object container = stack.getComponents().get(DataComponentTypes.CONTAINER);
            if (container == null) {
                return out;
            }

            for (String methodName : List.of("streamNonEmpty", "iterateNonEmpty", "stream")) {
                try {
                    var method = container.getClass().getMethod(methodName);
                    Object result = method.invoke(container);
                    if (result instanceof java.util.stream.Stream<?> stream) {
                        stream.forEach(obj -> {
                            if (obj instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                                out.add(itemStack.copy());
                            }
                        });
                        if (!out.isEmpty()) {
                            return out;
                        }
                    }
                    if (result instanceof Iterable<?> iterable) {
                        for (Object obj : iterable) {
                            if (obj instanceof ItemStack itemStack && !itemStack.isEmpty()) {
                                out.add(itemStack.copy());
                            }
                        }
                        if (!out.isEmpty()) {
                            return out;
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                    // Continue through known container API variants.
                }
            }
        } catch (Throwable ignored) {
            // Keep tooltip path safe when internals differ by mapping/build.
        }
        return out;
    }
}

