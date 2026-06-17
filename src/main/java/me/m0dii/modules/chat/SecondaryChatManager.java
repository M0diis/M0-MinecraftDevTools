package me.m0dii.modules.chat;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class SecondaryChatManager {
    private SecondaryChatManager() {
    }

    public static final class ChatMessage {
        private final long id;
        private final Text text;
        private final String plainText;
        private final Instant receivedAt;
        private int repeats = 1;

        private ChatMessage(long id, @NotNull Text text) {
            this.id = id;
            this.text = text.copy();
            this.plainText = this.text.getString();
            this.receivedAt = Instant.now();
        }

        public long id() {
            return id;
        }

        public Text text() {
            return text;
        }

        public String plainText() {
            return plainText;
        }

        public Instant receivedAt() {
            return receivedAt;
        }

        public int repeats() {
            return repeats;
        }
    }

    public static final class RouteResult {
        private final boolean routed;
        private final boolean matchedFilter;

        private RouteResult(boolean routed, boolean matchedFilter) {
            this.routed = routed;
            this.matchedFilter = matchedFilter;
        }

        public boolean routed() {
            return routed;
        }

        public boolean matchedFilter() {
            return matchedFilter;
        }
    }

    private static final Map<String, Deque<ChatMessage>> buffers = new LinkedHashMap<>();
    private static final Map<String, Double> scrollOffsets = new HashMap<>();
    private static final Map<String, Integer> unreadCounts = new HashMap<>();
    private static final Map<String, Pattern> patternCache = new HashMap<>();
    private static final String KEY_SEPARATOR = "\u001F";
    private static long nextMessageId = 1L;

    @Getter
    @Setter
    private static long lastAlphaReset = System.currentTimeMillis();

    public static synchronized void clear() {
        buffers.clear();
        scrollOffsets.clear();
        unreadCounts.clear();
    }

    public static synchronized void clear(@NotNull String windowId, @NotNull String tabId) {
        String key = key(windowId, tabId);
        buffers.remove(key);
        scrollOffsets.remove(key);
        unreadCounts.remove(key);
    }

    public static synchronized void clearWindow(@NotNull String windowId) {
        String prefix = windowId + KEY_SEPARATOR;
        buffers.keySet().removeIf(key -> key.startsWith(prefix));
        scrollOffsets.keySet().removeIf(key -> key.startsWith(prefix));
        unreadCounts.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static synchronized List<Text> snapshot() {
        SecondaryChatSettings.WindowConfig window = findWindow("main");
        if (window == null) {
            return List.of();
        }
        SecondaryChatSettings.TabConfig tab = selectedTab(window);
        if (tab == null) {
            return List.of();
        }

        List<Text> texts = new ArrayList<>();
        for (ChatMessage message : snapshot(window.id, tab.id)) {
            texts.add(message.text());
        }
        return texts;
    }

    public static synchronized List<ChatMessage> snapshot(@NotNull String windowId, @NotNull String tabId) {
        Deque<ChatMessage> buffer = buffers.get(key(windowId, tabId));
        if (buffer == null || buffer.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(buffer);
    }

    public static synchronized void push(@Nullable Text text) {
        if (text == null) {
            return;
        }

        SecondaryChatSettings.WindowConfig window = findWindow("main");
        if (window == null) {
            return;
        }
        SecondaryChatSettings.TabConfig tab = selectedTab(window);
        if (tab == null) {
            return;
        }
        addToTab(window, tab, text, SecondaryChatSettings.get().maxLines);
        resetScroll(window.id, tab.id);
        lastAlphaReset = System.currentTimeMillis();
    }

    public static synchronized RouteResult routeIncoming(@Nullable Text text) {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        if (!settings.enabled || text == null) {
            return new RouteResult(false, false);
        }

        boolean replaceMode = settings.renderMode == SecondaryChatSettings.RenderMode.REPLACE;
        boolean matchedFilter = matchesAnyConfiguredFilter(settings, text);
        if (!replaceMode && !matchedFilter) {
            return new RouteResult(false, false);
        }

        boolean routed = false;
        for (SecondaryChatSettings.WindowConfig window : settings.windows) {
            if (window == null) {
                continue;
            }
            List<SecondaryChatSettings.TabConfig> tabs = tabsForMessage(window, text, replaceMode);
            for (SecondaryChatSettings.TabConfig tab : tabs) {
                addToTab(window, tab, text, settings.maxLines);
                routed = true;
            }
        }

        if (routed) {
            lastAlphaReset = System.currentTimeMillis();
        }
        return new RouteResult(routed, matchedFilter);
    }

    public static boolean matchesFilter(@Nullable Text text) {
        SecondaryChatSettings.Data settings = SecondaryChatSettings.get();
        return settings.enabled && text != null && matchesAnyConfiguredFilter(settings, text);
    }

    public static synchronized double scrollOffset(@NotNull String windowId, @NotNull String tabId) {
        return scrollOffsets.getOrDefault(key(windowId, tabId), 0.0);
    }

    public static synchronized void scroll(@NotNull String windowId, @NotNull String tabId, double amount) {
        String key = key(windowId, tabId);
        double current = scrollOffsets.getOrDefault(key, 0.0);
        double max = Math.max(0, SecondaryChatSettings.get().maxLines * 20);
        scrollOffsets.put(key, Math.clamp(current + amount, 0, max));
    }

    public static synchronized void resetScroll() {
        scrollOffsets.replaceAll((key, value) -> 0.0);
    }

    public static synchronized void resetScroll(@NotNull String windowId, @NotNull String tabId) {
        scrollOffsets.put(key(windowId, tabId), 0.0);
    }

    public static synchronized int unreadCount(@NotNull String windowId, @NotNull String tabId) {
        return unreadCounts.getOrDefault(key(windowId, tabId), 0);
    }

    public static synchronized void clearUnread(@NotNull String windowId, @NotNull String tabId) {
        unreadCounts.remove(key(windowId, tabId));
    }

    public static synchronized void selectTab(@NotNull String windowId, @NotNull String tabId) {
        SecondaryChatSettings.updateAndSave(() -> {
            SecondaryChatSettings.WindowConfig window = findWindowMutable(windowId, SecondaryChatSettings.get().windows);
            if (window != null && findTabMutable(tabId, window.tabs) != null) {
                window.selectedTabId = tabId;
            }
        });
        clearUnread(windowId, tabId);
        resetScroll(windowId, tabId);
    }

    public static synchronized SecondaryChatSettings.WindowConfig findWindow(@NotNull String windowId) {
        return findWindowMutable(windowId, SecondaryChatSettings.get().windows);
    }

    public static synchronized SecondaryChatSettings.TabConfig selectedTab(@NotNull SecondaryChatSettings.WindowConfig window) {
        SecondaryChatSettings.TabConfig selected = findTabMutable(window.selectedTabId, window.tabs);
        if (selected != null) {
            return selected;
        }
        return window.tabs.isEmpty() ? null : window.tabs.getFirst();
    }

    public static synchronized SecondaryChatSettings.TabConfig findTab(@NotNull SecondaryChatSettings.WindowConfig window,
                                                                       @NotNull String tabId) {
        return findTabMutable(tabId, window.tabs);
    }

    private static void addToTab(SecondaryChatSettings.WindowConfig window,
                                 SecondaryChatSettings.TabConfig tab,
                                 Text text,
                                 int maxLines) {
        String key = key(window.id, tab.id);
        Deque<ChatMessage> buffer = buffers.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        if (!buffer.isEmpty()) {
            ChatMessage last = buffer.getLast();
            if (last.plainText().equals(text.getString()) && window.compactRepeats) {
                last.repeats++;
                resetScroll(window.id, tab.id);
                incrementUnreadIfNeeded(window, tab);
                return;
            }
        }

        buffer.addLast(new ChatMessage(nextMessageId++, text));
        while (buffer.size() > Math.max(1, maxLines)) {
            buffer.removeFirst();
        }
        resetScroll(window.id, tab.id);
        incrementUnreadIfNeeded(window, tab);
    }

    private static void incrementUnreadIfNeeded(SecondaryChatSettings.WindowConfig window,
                                                SecondaryChatSettings.TabConfig tab) {
        if (!tab.showNotifications || tab.id.equals(window.selectedTabId)) {
            return;
        }
        String key = key(window.id, tab.id);
        unreadCounts.put(key, unreadCounts.getOrDefault(key, 0) + 1);
    }

    private static List<SecondaryChatSettings.TabConfig> tabsForMessage(SecondaryChatSettings.WindowConfig window,
                                                                        Text text,
                                                                        boolean replaceMode) {
        List<SecondaryChatSettings.TabConfig> sorted = new ArrayList<>(window.tabs);
        sorted.sort(Comparator.comparingInt((SecondaryChatSettings.TabConfig tab) -> tab.priority).reversed());

        List<SecondaryChatSettings.TabConfig> matched = new ArrayList<>();
        Set<String> added = new HashSet<>();
        boolean stoppedBySkip = false;

        for (SecondaryChatSettings.TabConfig tab : sorted) {
            if (tab.alwaysAdd && added.add(tab.id)) {
                matched.add(tab);
            }
        }

        for (SecondaryChatSettings.TabConfig tab : sorted) {
            if (stoppedBySkip && !tab.alwaysAdd) {
                continue;
            }
            if (tab.catchAll && replaceMode) {
                if (added.add(tab.id)) {
                    matched.add(tab);
                }
                continue;
            }
            if (matchesTab(tab, text)) {
                if (added.add(tab.id)) {
                    matched.add(tab);
                }
                if (tab.skipOthers) {
                    stoppedBySkip = true;
                }
            }
        }

        if (matched.isEmpty()) {
            SecondaryChatSettings.TabConfig fallback = selectedTab(window);
            if (fallback != null) {
                matched.add(fallback);
            }
        }
        return matched;
    }

    private static boolean matchesAnyConfiguredFilter(SecondaryChatSettings.Data settings, Text text) {
        if (matchesRegexList(settings.regexList, text)) {
            return true;
        }

        for (SecondaryChatSettings.WindowConfig window : settings.windows) {
            if (window == null || window.tabs == null) {
                continue;
            }
            for (SecondaryChatSettings.TabConfig tab : window.tabs) {
                if (tab != null && !tab.catchAll && matchesTab(tab, text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesTab(SecondaryChatSettings.TabConfig tab, Text text) {
        return matchesRegexList(tab.regexList, text);
    }

    private static boolean matchesRegexList(@Nullable List<String> regexList, Text text) {
        if (regexList == null || regexList.isEmpty()) {
            return false;
        }

        String plain = text.getString();
        for (String regex : regexList) {
            Pattern p = getCompiled(regex);
            if (p != null && p.matcher(plain).find()) {
                return true;
            }
        }
        return false;
    }

    private static Pattern getCompiled(@NotNull String regex) {
        if (patternCache.containsKey(regex)) {
            return patternCache.get(regex);
        }
        try {
            Pattern pattern = Pattern.compile(regex);
            patternCache.put(regex, pattern);
            return pattern;
        } catch (PatternSyntaxException e) {
            patternCache.put(regex, null);
            return null;
        }
    }

    private static SecondaryChatSettings.WindowConfig findWindowMutable(String windowId,
                                                                        List<SecondaryChatSettings.WindowConfig> windows) {
        if (windows == null) {
            return null;
        }
        for (SecondaryChatSettings.WindowConfig window : windows) {
            if (window != null && window.id.equals(windowId)) {
                return window;
            }
        }
        return null;
    }

    private static SecondaryChatSettings.TabConfig findTabMutable(String tabId,
                                                                  List<SecondaryChatSettings.TabConfig> tabs) {
        if (tabs == null) {
            return null;
        }
        for (SecondaryChatSettings.TabConfig tab : tabs) {
            if (tab != null && tab.id.equals(tabId)) {
                return tab;
            }
        }
        return null;
    }

    private static String key(String windowId, String tabId) {
        return windowId + KEY_SEPARATOR + tabId;
    }
}
