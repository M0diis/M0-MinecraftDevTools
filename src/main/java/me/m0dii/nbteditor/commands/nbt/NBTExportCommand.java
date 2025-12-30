package me.m0dii.nbteditor.commands.nbt;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.m0dii.M0DevTools;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.localnbt.LocalBlock;
import me.m0dii.nbteditor.localnbt.LocalEntity;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.Version;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.tagreferences.TagNames;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.TextUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.PathUtil;

import java.io.File;
import java.nio.file.Files;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;
import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class NBTExportCommand extends ClientCommand {

    public static final NBTReferenceFilter EXPORT_FILTER = NBTReferenceFilter.create(
            ref -> true,
            ref -> true,
            ref -> true,
            TextInst.translatable("nbteditor.no_ref.to_export"),
            TextInst.translatable("nbteditor.no_hand.no_item.to_export"));

    public static final NBTReferenceFilter EXPORT_ITEM_FILTER = NBTReferenceFilter.create(
            null,
            ref -> true,
            ref -> true,
            TextInst.translatable("nbteditor.no_ref.to_export_item"),
            TextInst.translatable("nbteditor.requires_server"));

    private static final File exportDir = new File(M0DevToolsClient.SETTINGS_FOLDER, "exported");

    private static LocalEntity stripEntityTags(LocalEntity entity, String... tags) {
        LocalEntity output = entity.copy();
        stripEntityTags(output.getNBT(), tags);
        return output;
    }

    private static void stripEntityTags(NbtCompound nbt, String... tags) {
        for (String tag : tags)
            nbt.remove(tag);
        for (NbtElement passenger : nbt.getList("Passengers", NbtElement.COMPOUND_TYPE))
            stripEntityTags((NbtCompound) passenger, tags);
    }

    private static String getItemArgs(ItemStack item) {
        return MVRegistry.ITEM.getId(item.getItem()).toString() + NBTManagers.ITEM.getNbtString(item) + " " + item.getCount();
    }

    private static String getBlockArgs(LocalBlock block) {
        return block.getId().toString() + block.getState().toString() + (block.getNBT() == null ? "" : block.getNBT().asString());
    }

    private static String getEntityArgs(LocalEntity entity) {
        return entity.getId().toString() + " ~ ~ ~" + (entity.getNBT() == null ? "" : " " + entity.getNBT().asString());
    }

    private static String getCommand(String itemPrefix, String blockPrefix, String entityPrefix, LocalNBT nbt, boolean stripEntityUUIDs) {
        return switch (nbt) {
            case LocalItem item -> itemPrefix + getItemArgs(item.getReadableItem());
            case LocalBlock block -> blockPrefix + getBlockArgs(block);
            case LocalEntity entity ->
                    entityPrefix + getEntityArgs(stripEntityUUIDs ? stripEntityTags(entity, "UUID") : entity);
            case null, default -> throw new IllegalArgumentException("Cannot export " + nbt.getClass().getName());
        };
    }

    private static String getVanillaCommand(NBTReference<?> ref) {
        return getCommand("/give @p ", "/setblock ~ ~ ~ ", "/summon ", ref.getLocalNBT(), true);
    }

    private static String getGetCommand(NBTReference<?> ref) {
        return getCommand("/get item ", "/get block ~ ~ ~ ", "/get entity ", ref.getLocalNBT(), false);
    }

    private static void exportToClipboard(String str) {
        MiscUtil.client.keyboard.setClipboard(str);
        MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.nbt.export.copied"), false);
    }

    private static void exportToFile(NbtCompound nbt, String name) {
        try {
            if (!exportDir.exists()) {
                Files.createDirectory(exportDir.toPath());
            }
            File output = new File(exportDir, PathUtil.getNextUniqueName(exportDir.toPath(), name, ".nbt"));
            nbt.putInt("DataVersion", Version.getDataVersion());
            MVMisc.writeCompressedNbt(nbt, output);
            MiscUtil.client.player.sendMessage(TextUtil.attachFileTextOptions(TextInst.translatable("nbteditor.nbt.export.file.success",
                    TextInst.literal(output.getName()).formatted(Formatting.UNDERLINE).styled(style ->
                            style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, output.getAbsolutePath())))), output), false);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Error while exporting item", e);
            MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.nbt.export.file.error", e.getMessage()), false);
        }
    }

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(literal("cmd")
                        .executes(context -> {
                            NBTReference.getReference(EXPORT_FILTER, false, ref -> exportToClipboard(getVanillaCommand(ref)));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(literal("cmdblock").executes(context -> {
                    NBTReference.getReference(EXPORT_FILTER, false, ref -> {
                        ItemStack cmdBlock = new ItemStack(Items.COMMAND_BLOCK);
                        cmdBlock.manager$modifySubNbt(TagNames.BLOCK_ENTITY_TAG,
                                nbt -> MiscUtil.fillId(nbt, "minecraft:command_block").putString("Command", getVanillaCommand(ref)));
                        MiscUtil.getWithMessage(cmdBlock);
                    });
                    return Command.SINGLE_SUCCESS;
                })).then(literal("get").executes(context -> {
                    NBTReference.getReference(EXPORT_FILTER, false, ref -> exportToClipboard(getGetCommand(ref)));
                    return Command.SINGLE_SUCCESS;
                })).then(literal("item").executes(context -> {
                    NBTReference.getReference(EXPORT_ITEM_FILTER, false, ref -> {
                        LocalNBT localNBT = ref.getLocalNBT();

                        if (localNBT instanceof LocalEntity localEntity) {
                            localNBT = stripEntityTags(localEntity, "UUID", "Pos");
                        }

                        localNBT.toItem().ifPresentOrElse(MiscUtil::getWithMessage,
                                () -> MiscUtil.client.player.sendMessage(TextInst.translatable("nbteditor.nbt.export.item.error"), false));
                    });
                    return Command.SINGLE_SUCCESS;
                })).then(literal("file").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
                    NBTReference.getReference(EXPORT_FILTER, false, ref -> exportToFile(ref.getLocalNBT().serialize(),
                            context.getArgument("name", String.class)));
                    return Command.SINGLE_SUCCESS;
                })).executes(context -> {
                    NBTReference.getReference(EXPORT_FILTER, false, ref -> exportToFile(ref.getLocalNBT().serialize(),
                            ref.getLocalNBT().getName().getString() + "_" + MiscUtil.getFormattedCurrentTime()));
                    return Command.SINGLE_SUCCESS;
                }));
    }

}
