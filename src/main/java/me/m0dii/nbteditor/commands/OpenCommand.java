package me.m0dii.nbteditor.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.containers.ContainerIO;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.commands.FabricClientCommandSource;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.nbtreferences.NBTReferenceFilter;
import me.m0dii.nbteditor.packets.OpenEnderChestC2SPacket;
import me.m0dii.nbteditor.screens.containers.ContainerScreen;

import static me.m0dii.nbteditor.multiversion.commands.ClientCommandManager.literal;

public class OpenCommand extends ClientCommand {

    public static final OpenCommand INSTANCE = new OpenCommand();

    public static final NBTReferenceFilter CONTAINER_FILTER = NBTReferenceFilter.create(
            ref -> ContainerIO.isContainer(ref.getLocalNBT()),
            TextInst.translatable("nbteditor.no_ref.container"),
            TextInst.translatable("nbteditor.no_hand.no_item.container"));

    private OpenCommand() {

    }

    @Override
    public String getName() {
        return "open";
    }

    @Override
    public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
        builder.then(literal("echest").executes(context -> {

            if (M0DevToolsClient.SERVER_CONN.isEditingExpanded()) {
                ClientNetworking.send(new OpenEnderChestC2SPacket());
            } else {
                throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.requires_server")).create();
            }

            return Command.SINGLE_SUCCESS;
        })).executes(context -> {
            NBTReference.getReference(CONTAINER_FILTER, false, ContainerScreen::show);
            return Command.SINGLE_SUCCESS;
        });
    }

}
