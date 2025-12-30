package me.m0dii.nbteditor.commands.nbt;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.NBTEditorScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.command.argument.ItemStackArgument;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class NBTNewCommand extends ClientCommand {

    @Override
    public String getName() {
        return "new";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(argument("item", MVMisc.getItemStackArg()).executes(context -> {
            ItemReference ref = ItemReference.getHeldAir();
            ref.saveItem(context.getArgument("item", ItemStackArgument.class).createStack(1, true));
            MiscUtil.client.setScreen(new NBTEditorScreen<>(ref));
            return Command.SINGLE_SUCCESS;
        }));
    }

}
