package me.m0dii.modules.xray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class XrayManager {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-xray.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private static Set<String> xrayBlocks = new HashSet<>();

    public static void setXrayBlocks(Set<String> blocks) {
        xrayBlocks = blocks;
        save();
    }

    public static void addBlock(String blockId) {
        xrayBlocks.add(blockId);
        save();
    }

    public static void removeBlock(String blockId) {
        xrayBlocks.remove(blockId);
        save();
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }
        try {
            String json = Files.readString(CONFIG_FILE);
            Set<String> loaded = GSON.fromJson(json, Set.class);
            if (loaded != null) {
                xrayBlocks = new HashSet<>(loaded);
            }
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to load xray blocks: {}", e.getMessage());
        }
    }

    public static void save() {
        try {
            String json = GSON.toJson(xrayBlocks);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save xray blocks: {}", e.getMessage());
        }
    }
}

