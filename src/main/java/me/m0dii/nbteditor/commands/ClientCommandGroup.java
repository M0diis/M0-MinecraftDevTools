package me.m0dii.nbteditor.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;

import java.util.List;

public abstract class ClientCommandGroup extends ClientCommand {

    private final List<ClientCommand> children;

    protected ClientCommandGroup(List<ClientCommand> children) {
        this.children = children;
    }

    public final List<ClientCommand> getChildren() {
        return children;
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        for (ClientCommand child : children) {
            child.registerAll(builder::then, path + " " + child.getName());
        }
    }

    @Override
    public ClientCommand getShortcut(List<String> path, int index) {
        ClientCommand output = super.getShortcut(path, index);
        if (output != null || !allowShortcuts()) {
            return output;
        }

        if (path.size() > index) {
            String next = path.get(index);
            return children.stream()
                    .filter(cmd -> cmd.getName().equals(next))
                    .findFirst()
                    .map(cmd -> cmd.getShortcut(path, index + 1))
                    .orElse(null);
        }

        return null;
    }

    public boolean allowShortcuts() {
        return false;
    }

}
