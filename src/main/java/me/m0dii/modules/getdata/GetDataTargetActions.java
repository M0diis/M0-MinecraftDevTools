package me.m0dii.modules.getdata;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m0dii.utils.NbtEditorUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;

public final class GetDataTargetActions {
    private GetDataTargetActions() {
    }

    public static void saveTargetPayload(MinecraftClient client, String targetToken, String raw) throws CommandSyntaxException {
        if (client == null || client.player == null || targetToken == null || targetToken.isBlank()) {
            return;
        }

        NbtCompound merged = NbtEditorUtils.parseCompound(raw);
        if (targetToken.startsWith("block ")) {
            applyBlockStateAndNbtMerge(client, targetToken, merged);
            return;
        }

        String command = "data merge " + targetToken + " " + merged;
        sendCommand(client, command);
    }

    public static void saveTargetPathValue(MinecraftClient client, String targetToken, String raw, String path) throws CommandSyntaxException {
        if (client == null || client.player == null || targetToken == null || targetToken.isBlank()) {
            return;
        }
        NbtElement value = NbtEditorUtils.parseElement(raw);
        String command = "data modify " + targetToken + " " + path + " set value " + value;
        sendCommand(client, command);
    }

    private static void applyBlockStateAndNbtMerge(MinecraftClient client, String targetToken, NbtCompound merged) {
        if (client.player == null) {
            return;
        }
        String blockCoords = targetToken.substring("block ".length()).trim();

        String id = merged.contains("id") ? merged.getString("id").orElse("") : "";
        NbtCompound properties = merged.contains("Properties") && merged.get("Properties") instanceof NbtCompound compound ? compound : null;

        if (!id.isBlank()) {
            String stateSuffix = toBlockStateSuffix(properties);
            sendCommand(client, "setblock " + blockCoords + " " + id + stateSuffix);
        }

        NbtCompound forMerge = merged.copy();
        forMerge.remove("id");
        forMerge.remove("Properties");
        forMerge.remove("x");
        forMerge.remove("y");
        forMerge.remove("z");
        if (!forMerge.isEmpty()) {
            sendCommand(client, "data merge " + targetToken + " " + forMerge);
        }
    }

    private static void sendCommand(MinecraftClient client, String command) {
        if (client.player == null) {
            return;
        }
        client.player.networkHandler.sendChatCommand(command);
        client.player.sendMessage(Text.literal("[GetData] Sent: /" + command), false);
    }

    private static String toBlockStateSuffix(NbtCompound properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("[");
        boolean first = true;
        for (String key : properties.getKeys()) {
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
