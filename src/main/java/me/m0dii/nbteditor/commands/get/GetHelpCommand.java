package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.commands.arguments.EnumArgumentType;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.util.TextUtil;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class GetHelpCommand extends ClientCommand {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(argument("feature", EnumArgumentType.options(HelpType.class)).executes(context -> {
            context.getSource().sendFeedback(TextUtil.getLongTranslatableText(context.getArgument("feature", HelpType.class).msgKey));
            return Command.SINGLE_SUCCESS;
        })).executes(context -> {
            context.getSource().sendFeedback(TextUtil.getLongTranslatableText("nbteditor.help"));
            return Command.SINGLE_SUCCESS;
        });
    }

    public enum HelpType {
        NBTEDITOR("nbteditor.help.nbt"),
        OPEN("nbteditor.help.open"),
        GET("nbteditor.help.get"),
        FACTORIES("nbteditor.help.factories"),
        TEXTFORMAT("nbteditor.help.text_format");

        private final String msgKey;

        HelpType(String msgKey) {
            this.msgKey = msgKey;
        }
    }

}
