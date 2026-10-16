package me.m0dii.modules.scripting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.m0dii.utils.LegacyKeyCodes;

import java.util.Map;

/** On-disk format of {@code script_keybinds.json}. */
final class ScriptKeybindsFormat {
    /** 2 = key codes are SDL scancodes (Minecraft 26.x). */
    static final int FORMAT_VERSION = 2;

    private ScriptKeybindsFormat() {
    }

    /**
     * Fills {@code out} from the keybinds JSON. Files without {@code formatVersion} are the legacy bare map written
     * by 1.21.x builds (GLFW key codes); those are converted to SDL scancodes.
     *
     * @return whether the file was in the legacy format
     */
    static boolean parse(String json, Map<String, Integer> out) {
        JsonElement root = JsonParser.parseString(json);
        if (root == null || !root.isJsonObject()) {
            return false;
        }
        JsonObject object = root.getAsJsonObject();
        boolean legacy = !object.has("formatVersion");
        JsonObject bindings = legacy ? object : object.getAsJsonObject("keybinds");
        if (bindings == null) {
            return legacy;
        }
        for (Map.Entry<String, JsonElement> entry : bindings.entrySet()) {
            int key = entry.getValue().getAsInt();
            int converted = legacy ? LegacyKeyCodes.fromGlfw(key) : key;
            if (converted >= 0) {
                out.put(entry.getKey(), converted);
            }
        }
        return legacy;
    }
}
