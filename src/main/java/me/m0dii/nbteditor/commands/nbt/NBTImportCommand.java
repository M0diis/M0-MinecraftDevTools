package me.m0dii.nbteditor.commands.nbt;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.screens.ImportScreen;
import me.m0dii.nbteditor.util.MiscUtil;

public class NBTImportCommand extends ClientCommand {

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.executes(context -> {
            MiscUtil.client.setScreen(new ImportScreen());
            return Command.SINGLE_SUCCESS;
        });
    }

}
