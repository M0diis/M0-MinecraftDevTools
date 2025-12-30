package me.m0dii.modules.commandhistory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.m0dii.utils.ModConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CommandHistoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path HISTORY_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-command-history.json");
    private static final int MAX_HISTORY_SIZE = ModConfig.commandHistoryLimit;

    private static final LinkedList<String> commandHistory = new LinkedList<>();
    private static boolean loaded = false;

    public static void addCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        commandHistory.remove(command);
        commandHistory.addFirst(command);

        while (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.removeLast();
        }

        save();
    }

    public static List<String> getHistory() {
        if (!loaded) {
            load();
        }
        return new ArrayList<>(commandHistory);
    }

    public static void clearHistory() {
        commandHistory.clear();
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
                commandHistory.clear();
                commandHistory.addAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            String json = GSON.toJson(commandHistory);
            Files.writeString(HISTORY_FILE, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

