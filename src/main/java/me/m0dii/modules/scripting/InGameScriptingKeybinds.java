package me.m0dii.modules.scripting;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.m0dii.M0DevTools;
import me.m0dii.modules.scripting.gui.ScriptEditorScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class InGameScriptingKeybinds {
    private static final KeyBinding OPEN_SCRIPT_EDITOR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.m0-dev-tools.openscripteditor",
            GLFW.GLFW_KEY_COMMA,
            "category.m0-dev-tools.scripting"
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
            if (OPEN_SCRIPT_EDITOR.wasPressed()) {
                client.execute(() -> MinecraftClient.getInstance().setScreen(new ScriptEditorScreen()));
            }
            // Check script keybinds manually
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            for (Map.Entry<String, Integer> entry : scriptKeybinds.entrySet()) {
                String script = entry.getKey();
                int key = entry.getValue();
                boolean isPressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
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
            if (scriptName.endsWith(".groovy")) {
                GroovyScriptManager manager = new GroovyScriptManager();
                manager.runScript(script);
            } else if (scriptName.endsWith(".kts")) {
                KotlinScriptManager manager = new KotlinScriptManager();
                manager.runScript(script);
            } else {
                M0DevTools.LOGGER.warn("Unknown script extension for '{}'. Supported: .groovy, .kts", scriptName);
            }
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Error executing script '{}':", scriptName, e);
        }
    }

    private static void loadKeybinds() {
        scriptKeybinds.clear();
        if (Files.exists(KEYBINDS_FILE)) {
            try {
                String json = Files.readString(KEYBINDS_FILE);
                Map<String, Double> loaded = GSON.fromJson(json, new TypeToken<Map<String, Double>>() {
                }.getType());
                if (loaded != null) {
                    loaded.forEach((k, v) -> scriptKeybinds.put(k, v.intValue()));
                }
            } catch (IOException ignored) {
                // failed to read keybinds; nothing to load
            }
        }
    }

    private static void saveKeybinds() {
        try {
            Files.createDirectories(KEYBINDS_FILE.getParent());
            String json = GSON.toJson(scriptKeybinds);
            Files.writeString(KEYBINDS_FILE, json);
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
