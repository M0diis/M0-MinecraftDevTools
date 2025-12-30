package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommandGroup;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.screens.factories.DisplayScreen;
import me.m0dii.nbteditor.util.MiscUtil;

import java.util.List;

public class DisplayCommand extends ClientCommandGroup {

    public DisplayCommand() {
        super(List.of(
                new HideFlagsCommand(),
                new LoreCommand(),
                new NameCommand()));
    }

    @Override
    public String getName() {
        return "display";
    }

    @Override
    public boolean allowShortcuts() {
        return true;
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        super.register(builder, path);
        builder.executes(context -> {
            NBTReference.getReference(NBTReferenceFilter.ANY_NBT, false,
                    ref -> MiscUtil.client.setScreen(new DisplayScreen<>(ref)));
            return Command.SINGLE_SUCCESS;
        });
    }

}
