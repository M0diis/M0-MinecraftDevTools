package me.m0dii.nbteditor.commands.factories;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.M0DevTools;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.commands.ClientCommand;
import me.m0dii.nbteditor.commands.arguments.FancyTextArgumentType;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.util.StyleUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.argument;
import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class SignatureCommand extends ClientCommand {

    private static final File SIGNATURE_FILE = new File(M0DevToolsClient.SETTINGS_FOLDER, "signature.json");
    private static Text signature;

    static {
        if (!SIGNATURE_FILE.exists()) {
            signature = TextInst.translatable("nbteditor.sign.default");
        } else {
            try {
                signature = TextInst.fromJson(new String(Files.readAllBytes(SIGNATURE_FILE.toPath())));
            } catch (IOException | JsonParseException e) {
                M0DevTools.LOGGER.error("Error while loading signature", e);
                signature = TextInst.translatable("nbteditor.sign.load_error");
            }
        }
    }

    private static boolean hasSignature(List<Text> lore, Text signature) {
        if (lore.isEmpty()) {
            return false;
        }

        return lore.getLast().getString().equals(signature.getString());
    }

    private static boolean hasSignature(List<Text> lore) {
        return hasSignature(lore, signature);
    }

    @Override
    public String getName() {
        return "signature";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        Command<FabricClientCommandSource> addSignature = context -> {
            ItemReference ref = ItemReference.getHeldItem();
            ItemStack item = ref.getItem();

            List<Text> lore = ItemTagReferences.LORE.get(item);
            if (!hasSignature(lore)) {
                lore.add(signature);
            } else {
                context.getSource().sendFeedback(TextInst.translatable("nbteditor.sign.already_added"));
                return Command.SINGLE_SUCCESS;
            }

            ItemTagReferences.LORE.set(item, lore);
            ref.saveItem(item, TextInst.translatable("nbteditor.sign.added"));

            return Command.SINGLE_SUCCESS;
        };

        builder.executes(addSignature)
                .then(literal("add").executes(addSignature))
                .then(literal("remove").executes(context -> {
                    ItemReference ref = ItemReference.getHeldItem();
                    ItemStack item = ref.getItem();

                    List<Text> lore = ItemTagReferences.LORE.get(item);
                    if (!hasSignature(lore)) {
                        context.getSource().sendFeedback(TextInst.translatable("nbteditor.sign.not_added"));
                        return Command.SINGLE_SUCCESS;
                    }

                    lore.removeLast();
                    ItemTagReferences.LORE.set(item, lore);
                    ref.saveItem(item, TextInst.translatable("nbteditor.sign.removed"));

                    return Command.SINGLE_SUCCESS;
                }))
                .then(literal("edit").then(argument("signature", FancyTextArgumentType.fancyText(StyleUtil.BASE_LORE_STYLE)).executes(context -> {
                    Text oldSignature = signature;

                    try {
                        signature = context.getArgument("signature", Text.class);
                    } catch (IllegalArgumentException e) {
                        throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.sign.new.missing_arg")).create();
                    }
                    try {
                        Files.write(SIGNATURE_FILE.toPath(), TextInst.toJsonString(signature).getBytes());
                    } catch (IOException e) {
                        M0DevTools.LOGGER.error("Error while saving signature", e);
                        throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.sign.save_error")).create();
                    }

                    ItemReference ref = ItemReference.getHeldItem();
                    ItemStack item = ref.getItem();

                    List<Text> lore = ItemTagReferences.LORE.get(item);
                    if (hasSignature(lore, oldSignature)) {
                        lore.set(lore.size() - 1, signature);
                        ItemTagReferences.LORE.set(item, lore);
                        ref.saveItem(item, TextInst.translatable("nbteditor.sign.edited"));
                    }

                    return Command.SINGLE_SUCCESS;
                })));
    }

}
