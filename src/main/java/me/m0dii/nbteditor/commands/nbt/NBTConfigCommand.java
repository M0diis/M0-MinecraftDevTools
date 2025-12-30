package me.m0dii.nbteditor.commands.nbt;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.MiscUtil;

public class NBTConfigCommand extends ClientCommand {

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.executes(context -> {
            MiscUtil.client.setScreen(new ConfigScreen(null));
            return Command.SINGLE_SUCCESS;
        });
    }

}
