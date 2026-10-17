package me.m0dii.modules.state;

import com.google.gson.JsonObject;

import java.util.*;

/**
 * Everything saved in {@code config/m0-dev-tools-modules.json}: which modules are on, their settings,
 * favourites for ClickHUD and named profiles. Plain data, so it can be tested without a game.
 */
public final class ModuleStateData {
    public static final int FORMAT_VERSION = 1;

    public int formatVersion = FORMAT_VERSION;
    public Map<String, Boolean> enabled = new LinkedHashMap<>();
    public Map<String, JsonObject> settings = new LinkedHashMap<>();
    public List<String> favorites = new ArrayList<>();
    public Map<String, Profile> profiles = new TreeMap<>();
    public String activeProfile;

    /** A named snapshot of module states and settings. */
    public static final class Profile {
        public Map<String, Boolean> enabled = new LinkedHashMap<>();
        public Map<String, JsonObject> settings = new LinkedHashMap<>();

        public Profile copy() {
            Profile copy = new Profile();
            copy.enabled.putAll(enabled);
            settings.forEach((id, json) -> copy.settings.put(id, json == null ? null : json.deepCopy()));
            return copy;
        }
    }

    /** Fills in anything missing after loading an older or hand-edited file. */
    public ModuleStateData normalized() {
        if (enabled == null) {
            enabled = new LinkedHashMap<>();
        }
        if (settings == null) {
            settings = new LinkedHashMap<>();
        }
        if (favorites == null) {
            favorites = new ArrayList<>();
        }
        if (profiles == null) {
            profiles = new TreeMap<>();
        }
        enabled.values().removeIf(java.util.Objects::isNull);
        settings.values().removeIf(java.util.Objects::isNull);
        favorites.removeIf(id -> id == null || id.isBlank());
        profiles.values().removeIf(java.util.Objects::isNull);
        for (Profile profile : profiles.values()) {
            if (profile.enabled == null) {
                profile.enabled = new LinkedHashMap<>();
            }
            if (profile.settings == null) {
                profile.settings = new LinkedHashMap<>();
            }
        }
        formatVersion = FORMAT_VERSION;
        return this;
    }

    public static String normalizeProfileName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    public boolean isFavorite(String moduleId) {
        return moduleId != null && favorites.contains(moduleId);
    }

    /** Toggles a favourite and returns the new state. */
    public boolean toggleFavorite(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return false;
        }
        if (favorites.remove(moduleId)) {
            return false;
        }
        favorites.add(moduleId);
        return true;
    }

    /** Saves the current module states and settings as a profile. */
    public Profile saveProfile(String name) {
        Profile profile = new Profile();
        profile.enabled.putAll(enabled);
        settings.forEach((id, json) -> profile.settings.put(id, json.deepCopy()));
        profiles.put(normalizeProfileName(name), profile);
        activeProfile = normalizeProfileName(name);
        return profile;
    }

    /** Next profile name after the active one (wrapping), or {@code null} when there are no profiles. */
    public String nextProfileName() {
        if (profiles.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>(profiles.keySet());
        int index = activeProfile == null ? -1 : names.indexOf(activeProfile);
        return names.get((index + 1) % names.size());
    }
}
