package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.commands.arguments.FancyTextArgumentType;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.screens.factories.DisplayScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.text.Text;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;

public class NameCommand extends ClientCommand {

    @Override
    public String getName() {
        return "name";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(argument("name", FancyTextArgumentType.fancyText()).executes(context -> {
            Text name = context.getArgument("name", Text.class);
            NBTReference.getReference(NBTReferenceFilter.ANY_NBT, false,
                    ref -> ref.modifyLocalNBT(localNBT -> localNBT.setName(name),
                            TextInst.translatable("nbteditor.named").append(name)));

            return Command.SINGLE_SUCCESS;
        })).executes(context -> {
            NBTReference.getReference(NBTReferenceFilter.ANY_NBT, false,
                    ref -> MiscUtil.client.setScreen(new DisplayScreen<>(ref)));
            return Command.SINGLE_SUCCESS;
        });
    }

}
