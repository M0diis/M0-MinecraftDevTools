package me.m0dii.nbteditor.commands.get;

import me.m0dii.nbteditor.commands.ClientCommandGroup;

import java.util.ArrayList;
import java.util.List;

public class GetCommand extends ClientCommandGroup {

    public static final GetCommand INSTANCE = new GetCommand();

    private GetCommand() {
        super(new ArrayList<>(List.of(
                new GetItemCommand(),
                new GetBlockCommand(),
                new GetEntityCommand(),
                new GetPotionCommand(),
                new GetSkullCommand(),
                new GetPresetCommand(),
                new GetHelpCommand())));
    }

    @Override
    public String getName() {
        return "get";
    }

    @Override
    public boolean allowShortcuts() {
        return true;
    }

}
