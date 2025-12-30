package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.item.ItemStack;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class GetItemCommand extends ClientCommand {

    @Override
    public String getName() {
        return "item";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        Command<FabricClientCommandSource> getItem = context -> {
            int count = getDefaultArg(context, "count", 1, Integer.class);
            ItemStack item = context.getArgument("item", ItemStackArgument.class).createStack(count, false);
            MiscUtil.getWithMessage(item);
            return Command.SINGLE_SUCCESS;
        };

        builder.then(argument("item", MVMisc.getItemStackArg())
                .then(argument("count", IntegerArgumentType.integer(1)).executes(getItem)).executes(getItem));
    }

}
