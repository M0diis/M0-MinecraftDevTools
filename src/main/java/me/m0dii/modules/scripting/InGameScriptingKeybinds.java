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
import net.minecraft.network.chat.Component;
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
            pollScriptKeybinds(client);
        });
    }

    private static void pollScriptKeybinds(Minecraft client) {
        if (scriptKeybinds.isEmpty()) {
            keyPressedLastTick.clear();
            return;
        }

        // Read each key once per tick so several scripts bound to the same key all see the same press edge.
        Map<Integer, Boolean> pressedNow = new HashMap<>();
        for (int key : scriptKeybinds.values()) {
            if (key >= 0) {
                pressedNow.computeIfAbsent(key, k -> InputConstants.isKeyDown(k));
            }
        }

        // Don't fire while typing in chat, the script editor or any other screen. Key state is still tracked,
        // so closing a screen while a key is held does not trigger the script either.
        boolean canFire = client.player != null && client.gui.screen() == null;
        if (canFire) {
            for (Map.Entry<String, Integer> entry : scriptKeybinds.entrySet()) {
                int key = entry.getValue();
                boolean isPressed = pressedNow.getOrDefault(key, false);
                boolean wasPressed = keyPressedLastTick.getOrDefault(key, false);
                if (isPressed && !wasPressed) {
                    executeScriptByName(entry.getKey());
                }
            }
        }

        keyPressedLastTick.clear();
        keyPressedLastTick.putAll(pressedNow);
    }

    /**
     * Executes a script by name, using the correct script manager based on extension.
     */
    private static void executeScriptByName(String scriptName) {
        if (!ScriptTypes.isScriptFile(scriptName)) {
            M0DevTools.LOGGER.warn("Unknown script extension for '{}'. Supported: .groovy, .kts, .js", scriptName);
            return;
        }
        String script;
        try {
            script = ScriptStorage.readScript(scriptName);
        } catch (Exception e) {
            reportFailure("Script '" + scriptName + "' could not be read: " + e.getMessage());
            return;
        }
        ScriptRunner.run(scriptName, ScriptTypes.managerFor(scriptName), script, ScriptTypes.defaultContext(),
                ScriptDirectives.isAsync(script), outcome -> {
                    if (!outcome.success()) {
                        reportFailure(outcome.error());
                    }
                });
    }

    private static void reportFailure(String message) {
        M0DevTools.LOGGER.warn(message);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendOverlayMessage(Component.literal(message));
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
