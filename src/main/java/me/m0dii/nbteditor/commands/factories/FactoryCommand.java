package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommandGroup;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.screens.factories.LocalFactoryScreen;
import me.m0dii.nbteditor.util.MiscUtil;

import java.util.ArrayList;
import java.util.List;

public class FactoryCommand extends ClientCommandGroup {

    public static final FactoryCommand INSTANCE = new FactoryCommand();

    private FactoryCommand() {
        super(new ArrayList<>(List.of(
                new AttributesCommand(),
                new BlockStatesCommand(),
                new DisplayCommand(),
                new EnchantmentsCommand(),
                new MaxCommand(),
                new RandomUUIDCommand(),
                new SignatureCommand(),
                new UnbindSkullCommand(),
                new MaxStackSizeCommand(),
                new UnbreakableCommand())));
    }

    @Override
    public String getName() {
        return "factory";
    }

    @Override
    public boolean allowShortcuts() {
        return true;
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        super.register(builder, path);
        builder.executes(context -> {
            NBTReference.getReference(NBTReferenceFilter.ANY, false,
                    ref -> MiscUtil.client.setScreen(new LocalFactoryScreen<>(ref)));
            return Command.SINGLE_SUCCESS;
        });
    }

}
