package me.m0dii.nbteditor.commands.factories;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.factories.AttributesScreen;
import me.m0dii.nbteditor.server.ServerMiscUtil;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.tagreferences.specific.data.AttributeData;
import me.m0dii.nbteditor.tagreferences.specific.data.AttributeData.AttributeModifierData.AttributeModifierId;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class AttributesCommand extends ClientCommand {

    public static final NBTReferenceFilter ATTRIBUTES_FILTER = NBTReferenceFilter.create(
            ref -> true,
            null,
            ref -> ServerMiscUtil.createEntity(ref.getEntityType(), MiscUtil.client.world) instanceof MobEntity,
            TextInst.translatable("nbteditor.no_ref.attributes"),
            TextInst.translatable("nbteditor.no_hand.no_item.to_edit"));

    @Override
    public String getName() {
        return "attributes";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(literal("newuuids").executes(context -> {
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();
            List<AttributeData> attributes = ItemTagReferences.ATTRIBUTES.get(item);
            if (attributes.isEmpty()) {
                MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.attributes.new_uuids.no_attributes"), false);
            } else {
                attributes.replaceAll(attribute -> new AttributeData(attribute.attribute(), attribute.value(),
                        attribute.modifierData().get().operation(), attribute.modifierData().get().slot(), AttributeModifierId.randomUUID()));
                ItemTagReferences.ATTRIBUTES.set(item, attributes);
                ref.saveItem(item, () -> MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.attributes.new_uuids.success"), false));
            }

            return Command.SINGLE_SUCCESS;
        })).executes(context -> {
            NBTReference.getReference(ATTRIBUTES_FILTER, false, ref -> MiscUtil.client.setScreen(new AttributesScreen<>(ref)));
            return Command.SINGLE_SUCCESS;
        });
    }

}
