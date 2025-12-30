package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class RandomUUIDCommand extends ClientCommand {

    @Override
    public String getName() {
        return "randomuuid";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        Command<FabricClientCommandSource> add = context -> {
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();
            NbtCompound nbt = ItemTagReferences.CUSTOM_DATA.get(item);
            UUID uuid = UUID.randomUUID();
            nbt.putUuid("UUID", uuid);
            ItemTagReferences.CUSTOM_DATA.set(item, nbt);
            ref.saveItem(item, TextInst.translatable("nbteditor.random_uuid.added",
                    TextInst.literal(uuid.toString()).formatted(Formatting.GOLD)));
            return Command.SINGLE_SUCCESS;
        };
        Command<FabricClientCommandSource> remove = context -> {
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();
            NbtCompound nbt = ItemTagReferences.CUSTOM_DATA.get(item);
            if (!nbt.containsUuid("UUID")) {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.random_uuid.already_removed"), false);
                return Command.SINGLE_SUCCESS;
            }
            nbt.remove("UUID");
            ItemTagReferences.CUSTOM_DATA.set(item, nbt);
            ref.saveItem(item, TextInst.translatable("nbteditor.random_uuid.removed"));
            return Command.SINGLE_SUCCESS;
        };

        builder
                .then(literal("add").executes(add))
                .then(literal("remove").executes(remove))
                .executes(add);
    }

}
