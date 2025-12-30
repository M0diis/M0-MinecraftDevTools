package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import net.minecraft.item.ItemStack;

public class UnbreakableCommand extends ClientCommand {

    @Override
    public String getName() {
        return "unbreakable";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.executes(context -> {
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();
            boolean unbreakable = !ItemTagReferences.UNBREAKABLE.get(item);
            ItemTagReferences.UNBREAKABLE.set(item, unbreakable);
            ref.saveItem(item, TextInst.translatable("nbteditor.unbreakable." + (unbreakable ? "enabled" : "disabled")));
            return Command.SINGLE_SUCCESS;
        });
    }

}
