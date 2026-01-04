package me.m0dii.nbteditor.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.factories.FactoryCommand;
import me.m0dii.nbteditor.commands.get.GetCommand;
import me.m0dii.nbteditor.commands.nbt.NBTCommand;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.screens.ConfigScreen;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class NBTEditorCommands {

    private NBTEditorCommands() {
    }

    public static final Map<String, ClientCommand> COMMANDS = Stream.of(
                    NBTCommand.INSTANCE,
                    OpenCommand.INSTANCE,
                    GetCommand.INSTANCE,
                    FactoryCommand.INSTANCE)
            .collect(Collectors.toUnmodifiableMap(ClientCommand::getName, cmd -> cmd));

    public static void register() {
        MVMisc.registerCommands(dispatcher -> {
            for (ClientCommand cmd : COMMANDS.values()) {
                cmd.registerAll(dispatcher::register, cmd.getName());
            }

            for (String shortcut : ConfigScreen.getShortcuts()) {
                List<String> path = Arrays.asList(shortcut.split(" "));
                if (path.size() <= 1) {
                    continue;
                }

                ClientCommand cmd = COMMANDS.get(path.getFirst());

                if (cmd == null) {
                    continue;
                }

                cmd = cmd.getShortcut(path, 1);

                if (cmd == null) {
                    continue;
                }

                LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(path.getLast());
                cmd.register(builder, shortcut);
                dispatcher.register(builder);
            }
        });
    }
}
