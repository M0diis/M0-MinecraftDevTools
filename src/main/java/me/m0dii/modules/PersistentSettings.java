package me.m0dii.modules;

import com.google.gson.JsonObject;

/**
 * Implemented by modules whose settings (radius, display mode, ...) should survive restarts and be part of
 * profiles. {@link me.m0dii.modules.state.ModuleStateStore} calls these; modules never write files themselves.
 */
public interface PersistentSettings {
    /** Current settings as JSON. Return only what {@link #loadSettings(JsonObject)} understands. */
    JsonObject saveSettings();

    /** Applies saved settings. Must tolerate missing or out-of-range values. */
    void loadSettings(JsonObject settings);

    static int readInt(JsonObject json, String key, int fallback, int min, int max) {
        try {
            if (json != null && json.has(key)) {
                return Math.clamp(json.get(key).getAsInt(), min, max);
            }
        } catch (RuntimeException ignored) {
            // fall through to the default
        }
        return fallback;
    }

    static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        try {
            if (json != null && json.has(key)) {
                return json.get(key).getAsBoolean();
            }
        } catch (RuntimeException ignored) {
            // fall through to the default
        }
        return fallback;
    }

    static String readString(JsonObject json, String key, String fallback) {
        try {
            if (json != null && json.has(key)) {
                return json.get(key).getAsString();
            }
        } catch (RuntimeException ignored) {
            // fall through to the default
        }
        return fallback;
    }
}
