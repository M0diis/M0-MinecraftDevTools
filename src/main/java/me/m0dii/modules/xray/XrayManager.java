package me.m0dii.modules.xray;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class XrayManager {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-xray.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int MIN_DISPLAY_RANGE = 8;
    public static final int MAX_DISPLAY_RANGE = 96;
    public static final int DEFAULT_DISPLAY_RANGE = 24;

    @Getter
    private static Map<String, XrayBlockConfig> xrayBlocks = new LinkedHashMap<>();
    @Getter
    private static int displayRange = DEFAULT_DISPLAY_RANGE;

    public static class XrayBlockConfig {
        public boolean enabled = true;
        public int color = 0x55FF55;
        public boolean outline = true;
    }

    private XrayManager() {
    }

    public static void setDisplayRange(int range) {
        displayRange = Math.clamp(range, MIN_DISPLAY_RANGE, MAX_DISPLAY_RANGE);
        save();
    }

    public static void adjustDisplayRange(int delta) {
        setDisplayRange(displayRange + delta);
    }

    public static void setXrayBlocks(Map<String, XrayBlockConfig> blocks) {
        Map<String, XrayBlockConfig> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, XrayBlockConfig> entry : blocks.entrySet()) {
            String id = normalizeBlockId(entry.getKey());
            if (id != null) {
                normalized.put(id, entry.getValue() == null ? new XrayBlockConfig() : entry.getValue());
            }
        }
        xrayBlocks = normalized;
        ensureDefaultsIfEmpty();
        save();
    }

    public static void addBlock(String blockId) {
        String normalized = normalizeBlockId(blockId);
        if (normalized == null) {
            return;
        }
        xrayBlocks.computeIfAbsent(normalized, id -> new XrayBlockConfig());
        save();
    }

    public static void removeBlock(String blockId) {
        String normalized = normalizeBlockId(blockId);
        if (normalized == null) {
            return;
        }
        xrayBlocks.remove(normalized);
        save();
    }

    public static Set<String> getBlockIds() {
        return xrayBlocks.keySet();
    }

    public static XrayBlockConfig getOrCreate(String blockId) {
        String normalized = normalizeBlockId(blockId);
        if (normalized == null) {
            normalized = "minecraft:diamond_ore";
        }
        return xrayBlocks.computeIfAbsent(normalized, id -> new XrayBlockConfig());
    }

    public static void load() {
        if (!Files.exists(CONFIG_FILE)) {
            ensureDefaultsIfEmpty();
            displayRange = DEFAULT_DISPLAY_RANGE;
            save();
            return;
        }
        try {
            JsonElement root = GSON.fromJson(Files.readString(CONFIG_FILE), JsonElement.class);
            Map<String, XrayBlockConfig> normalized = new LinkedHashMap<>();
            int parsedRange = DEFAULT_DISPLAY_RANGE;

            if (root != null && root.isJsonObject() && root.getAsJsonObject().has("blocks")) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("displayRange")) {
                    parsedRange = Math.clamp(obj.get("displayRange").getAsInt(), MIN_DISPLAY_RANGE, MAX_DISPLAY_RANGE);
                }
                Map<String, XrayBlockConfig> loaded = GSON.fromJson(obj.get("blocks"), new TypeToken<Map<String, XrayBlockConfig>>() {}.getType());
                if (loaded != null) {
                    for (Map.Entry<String, XrayBlockConfig> entry : loaded.entrySet()) {
                        String id = normalizeBlockId(entry.getKey());
                        if (id != null) {
                            normalized.put(id, entry.getValue() == null ? new XrayBlockConfig() : entry.getValue());
                        }
                    }
                }
            } else {
                // Backward compatibility with map-only config format.
                Map<String, XrayBlockConfig> loaded = GSON.fromJson(root, new TypeToken<Map<String, XrayBlockConfig>>() {}.getType());
                if (loaded != null) {
                    for (Map.Entry<String, XrayBlockConfig> entry : loaded.entrySet()) {
                        String id = normalizeBlockId(entry.getKey());
                        if (id != null) {
                            normalized.put(id, entry.getValue() == null ? new XrayBlockConfig() : entry.getValue());
                        }
                    }
                }
            }

            xrayBlocks = normalized;
            displayRange = parsedRange;
            ensureDefaultsIfEmpty();
            save(); // Persist migrated IDs + new schema.
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to load xray blocks: {}", e.getMessage());
            ensureDefaultsIfEmpty();
            displayRange = DEFAULT_DISPLAY_RANGE;
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Invalid xray config format, resetting defaults: {}", e.getMessage());
            xrayBlocks.clear();
            ensureDefaultsIfEmpty();
            displayRange = DEFAULT_DISPLAY_RANGE;
            save();
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("displayRange", displayRange);
            root.add("blocks", GSON.toJsonTree(xrayBlocks));
            Files.writeString(CONFIG_FILE, GSON.toJson(root));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save xray blocks: {}", e.getMessage());
        }
    }

    private static void ensureDefaultsIfEmpty() {
        if (!xrayBlocks.isEmpty()) {
            return;
        }
        xrayBlocks.put("minecraft:diamond_ore", new XrayBlockConfig());
        xrayBlocks.put("minecraft:deepslate_diamond_ore", new XrayBlockConfig());
    }

    private static String normalizeBlockId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase();
        if (value.isEmpty()) {
            return null;
        }
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        Identifier id = Identifier.tryParse(value);
        return id == null ? null : id.toString();
    }
}
