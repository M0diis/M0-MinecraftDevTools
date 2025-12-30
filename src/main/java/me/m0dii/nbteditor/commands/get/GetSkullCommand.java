package me.m0dii.nbteditor.commands.get;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.StringHelper;

import java.util.Optional;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class GetSkullCommand extends ClientCommand {

    @Override
    public String getName() {
        return "skull";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(argument("player", StringArgumentType.word()).executes(context -> {
            String player = context.getArgument("player", String.class);

            if (!StringHelper.isValidPlayerName(player)) {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.skull.invalid_player_name"), false);
                return Command.SINGLE_SUCCESS;
            }

            ItemStack item = new ItemStack(Items.PLAYER_HEAD, 1);
            ItemTagReferences.PROFILE_NAME.set(item, Optional.of(player));
            MiscUtil.getWithMessage(item);

            return Command.SINGLE_SUCCESS;
        }));
    }

}
