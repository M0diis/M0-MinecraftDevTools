package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;
import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class MaxStackSizeCommand extends ClientCommand {

    @Override
    public String getName() {
        return "maxstacksize";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(literal("default").executes(context -> {
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();
            if (item.getComponentChanges().get(DataComponentTypes.MAX_STACK_SIZE) == null) {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.max_stack_size.already_removed"), false);
            } else if (item.contains(DataComponentTypes.MAX_DAMAGE) &&
                    item.getDefaultComponents().getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1) > 1) {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.max_stack_size.invalid_state"), false);
            } else {
                int size = item.getDefaultComponents().get(DataComponentTypes.MAX_STACK_SIZE);
                if (item.getCount() > size) {
                    item.setCount(size);
                }
                item.set(DataComponentTypes.MAX_STACK_SIZE, size);
                ref.saveItem(item, TextInst.translatable("nbteditor.max_stack_size.removed"));
            }
            return Command.SINGLE_SUCCESS;
        })).then(argument("size", IntegerArgumentType.integer(1, 99)).executes(context -> {
            int size = context.getArgument("size", Integer.class);
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();
            if (item.contains(DataComponentTypes.MAX_DAMAGE) && size > 1) {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.max_stack_size.invalid_state"), false);
            } else {
                if (item.getCount() > size) {
                    item.setCount(size);
                }
                item.set(DataComponentTypes.MAX_STACK_SIZE, size);
                ref.saveItem(item, TextInst.translatable("nbteditor.max_stack_size.added",
                        TextInst.literal(size + "").formatted(Formatting.GOLD)));
            }
            return Command.SINGLE_SUCCESS;
        }));
    }

}
