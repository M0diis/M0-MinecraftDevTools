package me.m0dii.modules.scripting;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.M0DevTools;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.scripting.gui.ScriptEditorScreen;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class InGameScriptingKeybinds {
    private static final KeyMapping OPEN_SCRIPT_EDITOR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            KeybindCatalog.OPEN_SCRIPT_EDITOR.translationKey(),
            KeybindCatalog.OPEN_SCRIPT_EDITOR.defaultKey(),
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "scripting"))
    ));
    private static final Path KEYBINDS_FILE = Paths.get("config/m0-dev-tools/script_keybinds.json");
    private static final Gson GSON = new Gson();

    private static final Map<String, Integer> scriptKeybinds = new HashMap<>();
    private static final Map<Integer, Boolean> keyPressedLastTick = new HashMap<>();

    private InGameScriptingKeybinds() {
        // Prevent instantiation
    }

    public static void register() {
        loadKeybinds();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_SCRIPT_EDITOR.consumeClick()) {
                client.execute(() -> Minecraft.getInstance().gui.setScreen(new ScriptEditorScreen()));
            }
            // Check script keybinds manually
            long window = Minecraft.getInstance().getWindow().handle();
            for (Map.Entry<String, Integer> entry : scriptKeybinds.entrySet()) {
                String script = entry.getKey();
                int key = entry.getValue();
                boolean isPressed = InputConstants.isKeyDown(key);
                boolean wasPressed = keyPressedLastTick.getOrDefault(key, false);
                if (isPressed && !wasPressed) {
                    executeScriptByName(script);
                }
                keyPressedLastTick.put(key, isPressed);
            }
        });
    }

    /**
     * Executes a script by name, using the correct script manager based on extension.
     */
    private static void executeScriptByName(String scriptName) {
        try {
            String script = ScriptStorage.readScript(scriptName);
            if (script == null) {
                M0DevTools.LOGGER.warn("Script '{}' returned null content", scriptName);
                return;
            }
            if (ScriptTypes.isScriptFile(scriptName)) {
                ScriptManager manager = ScriptTypes.managerFor(scriptName);
                manager.runScript(script, ScriptTypes.defaultContext());
            } else {
                M0DevTools.LOGGER.warn("Unknown script extension for '{}'. Supported: .groovy, .kts, .js", scriptName);
            }
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Error executing script '{}':", scriptName, e);
        }
    }

    private static void loadKeybinds() {
        scriptKeybinds.clear();
        if (!Files.exists(KEYBINDS_FILE)) {
            return;
        }
        try {
            boolean legacy = ScriptKeybindsFormat.parse(Files.readString(KEYBINDS_FILE), scriptKeybinds);
            if (legacy) {
                Path backup = KEYBINDS_FILE.resolveSibling(KEYBINDS_FILE.getFileName() + ".pre-26.bak");
                Files.copy(KEYBINDS_FILE, backup, StandardCopyOption.REPLACE_EXISTING);
                saveKeybinds();
                M0DevTools.LOGGER.info("Converted {} script key binding(s) to the 26.x key format (backup: {})",
                        scriptKeybinds.size(), backup.getFileName());
            }
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load script keybinds: {}", e.getMessage());
        }
    }

    private static void saveKeybinds() {
        try {
            Files.createDirectories(KEYBINDS_FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("formatVersion", ScriptKeybindsFormat.FORMAT_VERSION);
            root.add("keybinds", GSON.toJsonTree(scriptKeybinds));
            Files.writeString(KEYBINDS_FILE, GSON.toJson(root));
        } catch (IOException ignored) {
            // failed to save keybinds; ignore
        }
    }

    public static void registerKeybind(String scriptName, int keyCode) {
        scriptKeybinds.put(scriptName, keyCode);
        saveKeybinds();
    }

    public static void clearKeybind(String scriptName) {
        scriptKeybinds.remove(scriptName);
        saveKeybinds();
    }

    public static Integer getKeybindKey(String scriptName) {
        return scriptKeybinds.get(scriptName);
    }
}
