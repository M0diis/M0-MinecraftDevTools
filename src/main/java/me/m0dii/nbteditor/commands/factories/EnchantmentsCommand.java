package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.factories.EnchantmentsScreen;
import me.m0dii.nbteditor.util.MiscUtil;

public class EnchantmentsCommand extends ClientCommand {

    @Override
    public String getName() {
        return "enchantments";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.executes(context -> {
            MiscUtil.client.setScreen(new EnchantmentsScreen(ItemReference.getHeldItem()));
            return Command.SINGLE_SUCCESS;
        });
    }

}
