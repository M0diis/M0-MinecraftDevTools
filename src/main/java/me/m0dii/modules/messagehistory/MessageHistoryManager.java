package me.m0dii.modules.messagehistory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.m0dii.M0DevTools;
import me.m0dii.utils.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MessageHistoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path HISTORY_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-message-history.json");
    private static final int MAX_HISTORY_SIZE = ModConfig.messageHistoryLimit;

    private static final LinkedList<String> messageHistory = new LinkedList<>();
    private static boolean loaded = false;

    public static void addMessage(Text message) {
        if (message == null) {
            return;
        }

        String messageStr = message.getString();

        if (messageStr == null || messageStr.trim().isEmpty() && MessageHistoryModule.INSTANCE.isEnabled()) {
            return;
        }

        messageHistory.addFirst(messageStr);

        while (messageHistory.size() > MAX_HISTORY_SIZE) {
            messageHistory.removeLast();
        }

        save();
    }

    public static List<String> getHistory() {
        if (!loaded) {
            load();
        }
        return new ArrayList<>(messageHistory);
    }

    public static void clearHistory() {
        messageHistory.clear();
        save();
    }

    private static void load() {
        loaded = true;

        if (!Files.exists(HISTORY_FILE)) {
            return;
        }

        try {
            String json = Files.readString(HISTORY_FILE);
            List<String> loaded = GSON.fromJson(json, new TypeToken<List<String>>() {
            }.getType());
            if (loaded != null) {
                messageHistory.clear();
                messageHistory.addAll(loaded);
            }
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to load message history", e);
        }
    }

    private static void save() {
        try {
            String json = GSON.toJson(messageHistory);
            Files.writeString(HISTORY_FILE, json);
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save message history", e);
        }
    }
}

