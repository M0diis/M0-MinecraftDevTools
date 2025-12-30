package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.screens.factories.BlockStatesScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.item.BlockItem;

public class BlockStatesCommand extends ClientCommand {

    public static final NBTReferenceFilter BLOCK_FILTER = NBTReferenceFilter.create(
            ref -> ref.getItem().getItem() instanceof BlockItem,
            ref -> true,
            null,
            TextInst.translatable("nbteditor.no_ref.block"),
            TextInst.translatable("nbteditor.no_hand.no_item.block"));

    @Override
    public String getName() {
        return "blockstates";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.executes(context -> {
            NBTReference.getReference(BLOCK_FILTER, false, ref -> MiscUtil.client.setScreen(new BlockStatesScreen<>(ref)));
            return Command.SINGLE_SUCCESS;
        });
    }

}
