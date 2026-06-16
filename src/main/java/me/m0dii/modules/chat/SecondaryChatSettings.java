package me.m0dii.modules.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SecondaryChatSettings {
    public enum InterceptMode {
        COPY,
        MOVE
    }

    public enum RenderMode {
        ADDON,
        REPLACE
    }

    public static final class TabConfig {
        public String id = "all";
        public String name = "All";
        public List<String> regexList = new ArrayList<>();
        public int priority = 0;
        public boolean catchAll = true;
        public boolean alwaysAdd = false;
        public boolean skipOthers = false;
        public boolean showNotifications = true;
    }

    public static final class WindowConfig {
        public String id = "main";
        public String title = "Chat";
        public boolean visible = true;
        public boolean useHudCanvas = false;
        public int x = 8;
        public int y = 40;
        public int width = 320;
        public int height = 160;
        public int padding = 4;
        public int lineHeight = 9;
        public float fontScale = 0.85f;
        public int zIndex = 0;
        public int backgroundColor = 0x88000000;
        public int textColor = 0xFFE0E0E0;
        public int borderColor = 0xFFFFFFFF;
        public boolean drawBackground = true;
        public boolean drawBorder = false;
        public boolean showTabs = true;
        public boolean showTimestamps = false;
        public boolean compactRepeats = true;
        public String selectedTabId = "all";
        public List<TabConfig> tabs = new ArrayList<>();
    }

    public static final class Data {
        public int schemaVersion = 3;
        public boolean enabled = false;
        public RenderMode renderMode = RenderMode.REPLACE;
        public InterceptMode interceptMode = InterceptMode.COPY;
        public int maxLines = 100;
        public boolean showOverlay = true;
        public boolean showWhileGuiOpen = true;
        public boolean routeOutgoing = false;
        public List<String> regexList = new ArrayList<>();
        public String outgoingRegex = "";
        public boolean fadeEnabled = true;
        public int fadeDurationMs = 30000;
        public int minAlpha = 32;
        public boolean resetTransparencyWhenHovered = true;
        public boolean noTransparencyWhenChatOpen = true;
        public List<WindowConfig> windows = new ArrayList<>();
    }

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-secondary-chat.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Data data = new Data();
    private static boolean loaded = false;

    private SecondaryChatSettings() {
    }

    public static synchronized @NotNull Data get() {
        ensureLoaded();
        data = sanitize(data);
        return data;
    }

    public static synchronized void updateAndSave(@NotNull Runnable update) {
        ensureLoaded();
        update.run();
        data = sanitize(data);
        save();
    }

    public static synchronized void load() {
        loaded = true;
        if (!Files.exists(CONFIG_FILE)) {
            data = new Data();
            save();
            return;
        }

        try {
            Data loadedData = GSON.fromJson(Files.readString(CONFIG_FILE), Data.class);
            data = sanitize(loadedData == null ? new Data() : loadedData);
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load secondary chat settings: {}", e.getMessage());
            data = new Data();
        }
    }

    public static synchronized void save() {
        ensureLoaded();
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(data)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save secondary chat settings: {}", e.getMessage());
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            load();
        }
    }

    private static Data sanitize(Data raw) {
        Data clean = new Data();
        if (raw == null) {
            return clean;
        }
        clean.schemaVersion = 3;
        clean.enabled = raw.enabled;
        clean.renderMode = raw.renderMode == null || (raw.schemaVersion < 2 && raw.renderMode == RenderMode.ADDON)
                ? RenderMode.REPLACE
                : raw.renderMode;
        clean.interceptMode = raw.interceptMode == null ? InterceptMode.COPY : raw.interceptMode;
        clean.maxLines = Math.clamp(raw.maxLines, 10, 500);
        clean.showOverlay = true;
        clean.showWhileGuiOpen = raw.schemaVersion < 3 || raw.showWhileGuiOpen;
        clean.routeOutgoing = raw.routeOutgoing;
        clean.regexList = new ArrayList<>();
        if (raw.regexList != null) {
            for (String regex : raw.regexList) {
                if (regex == null) {
                    continue;
                }
                String trimmed = regex.trim();
                if (!trimmed.isEmpty()) {
                    clean.regexList.add(trimmed);
                }
            }
        }
        clean.outgoingRegex = raw.outgoingRegex == null ? "" : raw.outgoingRegex;
        clean.fadeEnabled = raw.fadeEnabled;
        clean.fadeDurationMs = Math.clamp(raw.fadeDurationMs, 1000, 120000);
        clean.minAlpha = Math.clamp(raw.minAlpha, 0, 255);
        clean.resetTransparencyWhenHovered = raw.resetTransparencyWhenHovered;
        clean.noTransparencyWhenChatOpen = raw.noTransparencyWhenChatOpen;
        clean.windows = sanitizeWindows(raw.windows, clean.regexList, raw.schemaVersion <= 0);
        return clean;
    }

    private static List<WindowConfig> sanitizeWindows(List<WindowConfig> rawWindows,
                                                      List<String> legacyRegexList,
                                                      boolean migrateLegacyRegexTab) {
        List<WindowConfig> cleaned = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        if (rawWindows != null) {
            for (WindowConfig window : rawWindows) {
                WindowConfig clean = sanitizeWindow(window, cleaned.size(), legacyRegexList, migrateLegacyRegexTab);
                if (clean != null && seenIds.add(clean.id)) {
                    cleaned.add(clean);
                }
            }
        }

        if (cleaned.isEmpty()) {
            cleaned.add(defaultMainWindow(legacyRegexList));
        }

        boolean hasMain = cleaned.stream().anyMatch(window -> "main".equals(window.id));
        if (!hasMain) {
            WindowConfig main = defaultMainWindow(legacyRegexList);
            main.id = "main";
            cleaned.addFirst(main);
        }

        return cleaned;
    }

    private static WindowConfig sanitizeWindow(WindowConfig raw,
                                               int index,
                                               List<String> legacyRegexList,
                                               boolean migrateLegacyRegexTab) {
        if (raw == null) {
            return null;
        }

        WindowConfig clean = new WindowConfig();
        clean.id = sanitizeId(raw.id, "window_" + (index + 1));
        clean.title = sanitizeLabel(raw.title, "Chat");
        clean.visible = raw.visible;
        clean.useHudCanvas = raw.useHudCanvas || ("main".equals(clean.id) && index == 0);
        clean.x = Math.clamp(raw.x, -10000, 10000);
        clean.y = Math.clamp(raw.y, -10000, 10000);
        clean.width = Math.clamp(raw.width, 120, 3000);
        clean.height = Math.clamp(raw.height, 60, 3000);
        clean.padding = Math.clamp(raw.padding, 0, 60);
        clean.lineHeight = Math.clamp(raw.lineHeight, 6, 40);
        clean.fontScale = Math.clamp(raw.fontScale, 0.25f, 5.0f);
        clean.zIndex = Math.clamp(raw.zIndex, -9999, 9999);
        clean.backgroundColor = raw.backgroundColor;
        clean.textColor = (raw.textColor >>> 24) == 0 ? 0xFFE0E0E0 : raw.textColor;
        clean.borderColor = (raw.borderColor >>> 24) == 0 ? 0xFFFFFFFF : raw.borderColor;
        clean.drawBackground = raw.drawBackground;
        clean.drawBorder = raw.drawBorder;
        clean.showTabs = raw.showTabs;
        clean.showTimestamps = raw.showTimestamps;
        clean.compactRepeats = raw.compactRepeats;
        clean.tabs = sanitizeTabs(raw.tabs, legacyRegexList, migrateLegacyRegexTab);
        clean.selectedTabId = sanitizeSelectedTab(raw.selectedTabId, clean.tabs);
        return clean;
    }

    private static List<TabConfig> sanitizeTabs(List<TabConfig> rawTabs,
                                                List<String> legacyRegexList,
                                                boolean migrateLegacyRegexTab) {
        List<TabConfig> cleaned = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        if (rawTabs != null) {
            for (TabConfig tab : rawTabs) {
                TabConfig clean = sanitizeTab(tab, cleaned.size());
                if (clean != null && seenIds.add(clean.id)) {
                    cleaned.add(clean);
                }
            }
        }

        if (cleaned.isEmpty()) {
            TabConfig all = new TabConfig();
            all.id = "all";
            all.name = "All";
            all.catchAll = true;
            cleaned.add(all);
        }

        if (migrateLegacyRegexTab
                && legacyRegexList != null && !legacyRegexList.isEmpty()
                && cleaned.stream().noneMatch(tab -> "secondary".equals(tab.id))) {
            TabConfig filtered = new TabConfig();
            filtered.id = "secondary";
            filtered.name = "Filtered";
            filtered.catchAll = false;
            filtered.priority = 10;
            filtered.regexList = new ArrayList<>(legacyRegexList);
            cleaned.add(filtered);
        }

        boolean hasCatchAll = cleaned.stream().anyMatch(tab -> tab.catchAll);
        if (!hasCatchAll) {
            cleaned.getFirst().catchAll = true;
        }

        return cleaned;
    }

    private static TabConfig sanitizeTab(TabConfig raw, int index) {
        if (raw == null) {
            return null;
        }

        TabConfig clean = new TabConfig();
        clean.id = sanitizeId(raw.id, "tab_" + (index + 1));
        clean.name = sanitizeLabel(raw.name, clean.id);
        clean.priority = Math.clamp(raw.priority, -9999, 9999);
        clean.catchAll = raw.catchAll;
        clean.alwaysAdd = raw.alwaysAdd;
        clean.skipOthers = raw.skipOthers;
        clean.showNotifications = raw.showNotifications;
        clean.regexList = sanitizeRegexList(raw.regexList);
        return clean;
    }

    private static List<String> sanitizeRegexList(List<String> raw) {
        List<String> cleaned = new ArrayList<>();
        if (raw == null) {
            return cleaned;
        }
        for (String regex : raw) {
            if (regex == null) {
                continue;
            }
            String trimmed = regex.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }

    private static WindowConfig defaultMainWindow(List<String> legacyRegexList) {
        WindowConfig window = new WindowConfig();
        window.id = "main";
        window.title = "Chat";
        window.useHudCanvas = true;
        window.tabs = sanitizeTabs(null, legacyRegexList, true);
        window.selectedTabId = "all";
        return window;
    }

    private static String sanitizeSelectedTab(String selectedTabId, List<TabConfig> tabs) {
        String selected = sanitizeId(selectedTabId, "");
        if (!selected.isEmpty()) {
            for (TabConfig tab : tabs) {
                if (tab.id.equals(selected)) {
                    return selected;
                }
            }
        }
        return tabs.isEmpty() ? "all" : tabs.getFirst().id;
    }

    private static String sanitizeId(String id, String fallback) {
        String candidate = id == null ? "" : id.trim().toLowerCase();
        if (candidate.isEmpty()) {
            candidate = fallback;
        }
        candidate = candidate.replaceAll("[^a-z0-9_.-]", "_");
        return candidate.isEmpty() ? fallback : candidate;
    }

    private static String sanitizeLabel(String label, String fallback) {
        String candidate = label == null ? "" : label.trim();
        if (candidate.isEmpty()) {
            return fallback;
        }
        return candidate.length() > 32 ? candidate.substring(0, 32) : candidate;
    }
}

