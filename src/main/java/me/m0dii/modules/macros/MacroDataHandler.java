package me.m0dii.modules.macros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Builder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MacroDataHandler {

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-macros.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, MacroEntry> macros = new HashMap<>();

    @Builder
    public static class MacroEntry {
        public final String name;
        public final List<String> commands;
        public final int keyCode;
        public final String modifierKey;
        public final int delayTicks;
        public final boolean showInOverlay;
    }

    public static void loadMacros() {
        if (!Files.exists(CONFIG_FILE)) {
            saveMacros();
            return;
        }

        try {
            String json = Files.readString(CONFIG_FILE);
            TypeToken<Map<String, MacroEntry>> typeToken = new TypeToken<>() {
            };
            Map<String, MacroEntry> loadedMacros = GSON.fromJson(json, typeToken.getType());
            if (loadedMacros != null) {
                macros = loadedMacros;
            }
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to load macros: {}", e.getMessage());
        }
    }

    public static void saveMacros() {
        try {
            String json = GSON.toJson(macros);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save macros: {}", e.getMessage());
        }
    }

    public static void addMacro(String id,
                                String name,
                                List<String> commands,
                                int keyCode,
                                String modifierKey,
                                int delayTicks,
                                boolean showInOverlay) {
        macros.put(id, MacroEntry.builder()
                .name(name)
                .commands(commands != null ? commands : new ArrayList<>())
                .keyCode(keyCode)
                .modifierKey(modifierKey)
                .delayTicks(delayTicks)
                .showInOverlay(showInOverlay)
                .build());

        saveMacros();
    }

    public static void removeMacro(String id) {
        macros.remove(id);
        saveMacros();
    }

    public static MacroEntry getMacro(String id) {
        return macros.get(id);
    }

    public static Map<String, MacroEntry> getAllMacros() {
        return new HashMap<>(macros);
    }

    public static boolean hasMacro(String id) {
        return macros.containsKey(id);
    }

    public static void updateMacro(String id,
                                   String name,
                                   List<String> commands,
                                   int keyCode,
                                   String modifierKey,
                                   int delayTicks,
                                   boolean showInOverlay) {
        if (macros.containsKey(id)) {
            addMacro(id, name, commands, keyCode, modifierKey, delayTicks, showInOverlay);
        }
    }
}
