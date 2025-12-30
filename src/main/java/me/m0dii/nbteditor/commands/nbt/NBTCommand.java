package me.m0dii.nbteditor.commands.nbt;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommandGroup;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.screens.NBTEditorScreen;
import me.m0dii.nbteditor.util.MiscUtil;

import java.util.ArrayList;
import java.util.List;

public class NBTCommand extends ClientCommandGroup {

    public static final NBTCommand INSTANCE = new NBTCommand();

    private NBTCommand() {
        super(new ArrayList<>(List.of(
                new NBTConfigCommand(),
                new NBTNewCommand(),
                new NBTExportCommand(),
                new NBTImportCommand())));
    }

    @Override
    public String getName() {
        return "nbteditor";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        super.register(builder, path);
        builder.executes(context -> {
            NBTReference.getReference(NBTReferenceFilter.ANY, ConfigScreen.isAirEditable(),
                    ref -> MiscUtil.client.setScreen(new NBTEditorScreen<>(ref)));
            return Command.SINGLE_SUCCESS;
        });
    }

    @Override
    public boolean allowShortcuts() {
        return true;
    }

}
