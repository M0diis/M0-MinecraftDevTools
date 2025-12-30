package me.m0dii.nbteditor.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.screens.ConfigScreen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public abstract class ClientCommand {

    protected ClientCommand() {
    }

    public static <T> T getDefaultArg(CommandContext<FabricClientCommandSource> context, String name, T defaultValue, Class<T> type) {
        try {
            return context.getArgument(name, type);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public void registerAll(Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> commandHandler, String path) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(getName());
        register(builder, path);
        commandHandler.accept(builder);

        Set<String> aliases = new HashSet<>();
        for (ConfigScreen.Alias alias : ConfigScreen.getAliases()) {
            if (alias.original().equals(path)) {
                aliases.add(alias.alias());
            }
        }

        for (String alias : aliases) {
            builder = literal(alias);
            register(builder, path);
            commandHandler.accept(builder);
        }
    }

    public abstract String getName();

    public abstract void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path);

    public ClientCommand getShortcut(List<String> path, int index) {
        return path.size() == index ? this : null;
    }

}
