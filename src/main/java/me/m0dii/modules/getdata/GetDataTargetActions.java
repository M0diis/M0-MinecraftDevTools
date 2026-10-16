package me.m0dii.modules.getdata;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.utils.NbtEditorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

public final class GetDataTargetActions {
    private GetDataTargetActions() {
    }

    public static void saveTargetPayload(Minecraft client, String targetToken, String raw) throws CommandSyntaxException {
        if (client == null || client.player == null || targetToken == null || targetToken.isBlank()) {
            return;
        }

        CompoundTag merged = NbtEditorUtils.parseCompound(raw);
        if (targetToken.startsWith("block ")) {
            applyBlockStateAndNbtMerge(client, targetToken, merged);
            return;
        }

        String command = "data merge " + targetToken + " " + merged;
        sendCommand(client, command);
    }

    public static void saveTargetPathValue(Minecraft client, String targetToken, String raw, String path) throws CommandSyntaxException {
        if (client == null || client.player == null || targetToken == null || targetToken.isBlank()) {
            return;
        }
        Tag value = NbtEditorUtils.parseElement(raw);
        String command = "data modify " + targetToken + " " + path + " set value " + value;
        sendCommand(client, command);
    }

    private static void applyBlockStateAndNbtMerge(Minecraft client, String targetToken, CompoundTag merged) {
        if (client.player == null) {
            return;
        }
        String blockCoords = targetToken.substring("block ".length()).trim();

        String id = merged.contains("id") ? merged.getString("id").orElse("") : "";
        CompoundTag properties = merged.contains("Properties") && merged.get("Properties") instanceof CompoundTag compound ? compound : null;

        if (!id.isBlank()) {
            String stateSuffix = toBlockStateSuffix(properties);
            sendCommand(client, "setblock " + blockCoords + " " + id + stateSuffix);
        }

        CompoundTag forMerge = merged.copy();
        forMerge.remove("id");
        forMerge.remove("Properties");
        forMerge.remove("x");
        forMerge.remove("y");
        forMerge.remove("z");
        if (!forMerge.isEmpty()) {
            sendCommand(client, "data merge " + targetToken + " " + forMerge);
        }
    }

    private static void sendCommand(Minecraft client, String command) {
        if (client.player == null) {
            return;
        }
        client.player.connection.sendCommand(command);
        client.player.sendSystemMessage(Component.literal("[GetData] Sent: /" + command));
    }

    private static String toBlockStateSuffix(CompoundTag properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (String key : properties.keySet()) {
            String value = properties.getString(key).orElse("");
            if (value.isBlank()) {
                continue;
            }
            if (!first) {
                builder.append(',');
            }
            builder.append(key).append('=').append(value.replace('"', ' ').trim());
            first = false;
        }
        builder.append(']');
        return first ? "" : builder.toString();
    }
}
