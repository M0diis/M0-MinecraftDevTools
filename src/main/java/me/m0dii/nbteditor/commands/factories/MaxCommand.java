package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.MVEnchantments;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.tagreferences.specific.data.Enchants;
import net.minecraft.item.ItemStack;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;
import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class MaxCommand extends ClientCommand {

    @Override
    public String getName() {
        return "max";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        int maxLevel = 255;

        builder
                .then(literal("cursed")
                        .then(literal("all")
                                .then(argument("level", IntegerArgumentType.integer(1, maxLevel))
                                        .executes(context -> max(context.getArgument("level", Integer.class), true, true)))
                                .executes(context -> max(-1, true, true)))
                        .then(argument("level", IntegerArgumentType.integer(1, maxLevel))
                                .executes(context -> max(context.getArgument("level", Integer.class), false, true)))
                        .executes(context -> max(-1, false, true)))
                .then(literal("all")
                        .then(argument("level", IntegerArgumentType.integer(1, maxLevel))
                                .executes(context -> max(context.getArgument("level", Integer.class), true, false)))
                        .executes(context -> max(-1, true, false)))
                .then(argument("level", IntegerArgumentType.integer(1, maxLevel))
                        .executes(context -> max(context.getArgument("level", Integer.class), false, false)))
                .executes(context -> max(-1, false, false));
    }

    private int max(int enchantLevel, boolean allEnchants, boolean cursed) throws CommandSyntaxException {
        ItemReference ref = ItemReference.getHeldItem();
        ItemStack item = ref.getItem();
        Enchants enchants = ItemTagReferences.ENCHANTMENTS.get(item);

        enchants.removeDuplicates();

        MVRegistry.getEnchantmentRegistry().forEach(enchant -> {
            if ((allEnchants || enchant.isAcceptableItem(item)) && (cursed || !MVEnchantments.isCursed(enchant))) {
                enchants.setEnchant(enchant, enchantLevel == -1 ? enchant.getMaxLevel() : enchantLevel, true);
            }
        });

        ItemTagReferences.ENCHANTMENTS.set(item, enchants);

        ref.saveItem(item, TextInst.translatable("nbteditor.maxed"));

        return Command.SINGLE_SUCCESS;
    }

}
