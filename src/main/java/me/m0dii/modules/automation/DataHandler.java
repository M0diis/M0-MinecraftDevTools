package me.m0dii.modules.automation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.m0dii.M0DevTools;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class DataHandler {
    public static final int CURRENT_VERSION = 1;

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("m0-dev-tools-automation.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static AutomationConfig config = defaults();

    private DataHandler() {
    }

    public static final class GlobalRateLimit {
        public int maxExecutions = 25;
        public long windowMs = 5_000L;
    }

    public static final class AutomationConfig {
        public int version = CURRENT_VERSION;
        public GlobalRateLimit globalRateLimit = new GlobalRateLimit();
        public int loopPreventionBurstCount = 5;
        public long loopPreventionWindowMs = 1_500L;
        public List<AutomationRule> rules = new ArrayList<>();
    }

    public static synchronized void load() {
        if (!Files.exists(CONFIG_FILE)) {
            config = defaults();
            save();
            return;
        }

        try {
            AutomationConfig loaded = GSON.fromJson(Files.readString(CONFIG_FILE), AutomationConfig.class);
            config = sanitize(loaded);
            save();
        } catch (Exception e) {
            M0DevTools.LOGGER.error("Failed to load automation config: {}", e.getMessage());
            config = defaults();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Files.writeString(CONFIG_FILE, GSON.toJson(sanitize(config)));
        } catch (IOException e) {
            M0DevTools.LOGGER.error("Failed to save automation config: {}", e.getMessage());
        }
    }

    public static synchronized @NotNull AutomationConfig getConfigCopy() {
        return deepCopy(sanitize(config));
    }

    public static synchronized void setConfig(@NotNull AutomationConfig next) {
        config = sanitize(next);
        save();
    }

    public static Path configPath() {
        return CONFIG_FILE;
    }

    public static String exampleJson() {
        return GSON.toJson(defaultExample());
    }

    private static AutomationConfig sanitize(AutomationConfig raw) {
        AutomationConfig cleaned = defaults();
        if (raw == null) {
            return cleaned;
        }

        cleaned.version = CURRENT_VERSION;
        cleaned.globalRateLimit = sanitizeGlobalRateLimit(raw.globalRateLimit);
        cleaned.loopPreventionBurstCount = Math.clamp(raw.loopPreventionBurstCount, 1, 100);
        cleaned.loopPreventionWindowMs = Math.clamp(raw.loopPreventionWindowMs, 250L, 300_000L);
        cleaned.rules = sanitizeRules(raw.rules);
        return cleaned;
    }

    private static GlobalRateLimit sanitizeGlobalRateLimit(GlobalRateLimit raw) {
        GlobalRateLimit limit = new GlobalRateLimit();
        if (raw == null) {
            return limit;
        }
        limit.maxExecutions = Math.clamp(raw.maxExecutions, 1, 1_000);
        limit.windowMs = Math.clamp(raw.windowMs, 250L, 3_600_000L);
        return limit;
    }

    private static List<AutomationRule> sanitizeRules(List<AutomationRule> rawRules) {
        List<AutomationRule> rules = new ArrayList<>();
        if (rawRules == null) {
            return rules;
        }

        Set<String> seenIds = new LinkedHashSet<>();
        for (AutomationRule raw : rawRules) {
            if (raw == null) {
                continue;
            }
            AutomationRule rule = sanitizeRule(raw, seenIds);
            rules.add(rule);
        }
        rules.sort((left, right) -> Integer.compare(right.priority, left.priority));
        return rules;
    }

    private static AutomationRule sanitizeRule(AutomationRule raw, Set<String> seenIds) {
        AutomationRule cleaned = new AutomationRule();
        cleaned.id = sanitizeId(raw.id, seenIds);
        cleaned.name = sanitizeText(raw.name, "Rule");
        cleaned.enabled = raw.enabled;
        cleaned.eventType = raw.eventType == null ? AutomationEventType.TICK_INTERVAL : raw.eventType;
        cleaned.cooldownMs = Math.max(0L, raw.cooldownMs);
        cleaned.debounceMs = Math.max(0L, raw.debounceMs);
        cleaned.rateLimitCount = Math.max(0, raw.rateLimitCount);
        cleaned.rateLimitWindowMs = raw.rateLimitCount <= 0 ? 0L : Math.max(0L, raw.rateLimitWindowMs);
        cleaned.priority = Math.clamp(raw.priority, -10_000, 10_000);
        cleaned.eventFilters = sanitizeFilters(raw.eventFilters);
        cleaned.conditions = sanitizeConditions(raw.conditions);
        cleaned.actions = sanitizeActions(raw.actions);
        return cleaned;
    }

    private static List<AutomationRule.EventFilter> sanitizeFilters(List<AutomationRule.EventFilter> rawFilters) {
        List<AutomationRule.EventFilter> filters = new ArrayList<>();
        if (rawFilters == null) {
            return filters;
        }
        for (AutomationRule.EventFilter raw : rawFilters) {
            if (raw == null) {
                continue;
            }
            String field = sanitizeText(raw.field, "");
            if (field.isBlank()) {
                continue;
            }
            AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
            filter.field = field;
            filter.operator = raw.operator == null ? AutomationRule.Operator.EQUALS : raw.operator;
            filter.value = raw.value == null ? "" : raw.value;
            filter.ignoreCase = raw.ignoreCase;
            filters.add(filter);
        }
        return filters;
    }

    private static List<AutomationRule.Condition> sanitizeConditions(List<AutomationRule.Condition> rawConditions) {
        List<AutomationRule.Condition> conditions = new ArrayList<>();
        if (rawConditions == null) {
            return conditions;
        }
        for (AutomationRule.Condition raw : rawConditions) {
            if (raw == null) {
                continue;
            }
            String field = sanitizeText(raw.field, "");
            if (field.isBlank()) {
                continue;
            }
            AutomationRule.Condition condition = new AutomationRule.Condition();
            condition.source = raw.source == null ? AutomationRule.ConditionSource.EVENT : raw.source;
            condition.field = field;
            condition.operator = raw.operator == null ? AutomationRule.Operator.EQUALS : raw.operator;
            condition.value = raw.value == null ? "" : raw.value;
            condition.ignoreCase = raw.ignoreCase;
            conditions.add(condition);
        }
        return conditions;
    }

    private static List<AutomationRule.Action> sanitizeActions(List<AutomationRule.Action> rawActions) {
        List<AutomationRule.Action> actions = new ArrayList<>();
        if (rawActions == null) {
            return actions;
        }
        for (AutomationRule.Action raw : rawActions) {
            if (raw == null) {
                continue;
            }
            String target = sanitizeText(raw.target, "");
            if (target.isBlank()) {
                continue;
            }
            AutomationRule.Action action = new AutomationRule.Action();
            action.type = raw.type == null ? AutomationActionType.SEND_CLIENT_COMMAND : raw.type;
            action.target = target;
            action.argument = raw.argument == null ? "" : raw.argument.trim();
            action.enabled = raw.enabled;
            action.parameters = sanitizeParameters(raw.parameters);
            actions.add(action);
        }
        return actions;
    }

    private static Map<String, String> sanitizeParameters(Map<String, String> rawParameters) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (rawParameters == null) {
            return parameters;
        }
        rawParameters.forEach((key, value) -> {
            String cleanedKey = sanitizeText(key, "");
            if (!cleanedKey.isBlank()) {
                parameters.put(cleanedKey, value == null ? "" : value.trim());
            }
        });
        return parameters;
    }

    private static String sanitizeId(String rawId, Set<String> seenIds) {
        String normalized = sanitizeText(rawId, AutomationRule.newId())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "_");
        if (normalized.isBlank()) {
            normalized = AutomationRule.newId();
        }

        String candidate = normalized;
        int suffix = 2;
        while (seenIds.contains(candidate)) {
            candidate = normalized + "_" + suffix++;
        }
        seenIds.add(candidate);
        return candidate;
    }

    private static String sanitizeText(String raw, String fallback) {
        String text = raw == null ? "" : raw.trim();
        return text.isBlank() ? fallback : text;
    }

    private static AutomationConfig defaults() {
        AutomationConfig defaults = new AutomationConfig();
        defaults.version = CURRENT_VERSION;
        defaults.globalRateLimit = new GlobalRateLimit();
        defaults.loopPreventionBurstCount = 5;
        defaults.loopPreventionWindowMs = 1_500L;
        defaults.rules = new ArrayList<>();
        return defaults;
    }

    private static AutomationConfig deepCopy(AutomationConfig source) {
        AutomationConfig copy = new AutomationConfig();
        copy.version = source.version;
        copy.globalRateLimit = sanitizeGlobalRateLimit(source.globalRateLimit);
        copy.loopPreventionBurstCount = source.loopPreventionBurstCount;
        copy.loopPreventionWindowMs = source.loopPreventionWindowMs;
        copy.rules = new ArrayList<>();
        for (AutomationRule rule : source.rules) {
            copy.rules.add(rule.copy());
        }
        return copy;
    }

    private static AutomationConfig defaultExample() {
        AutomationConfig example = defaults();

        AutomationRule joinRule = AutomationRule.createDefault();
        joinRule.id = "welcome_join";
        joinRule.name = "Welcome Join";
        joinRule.eventType = AutomationEventType.WORLD_JOIN;
        joinRule.actions = List.of(AutomationRule.Action.command("/say automation online"));

        AutomationRule chatRule = AutomationRule.createDefault();
        chatRule.id = "chat_match_macro";
        chatRule.name = "Chat Regex Macro";
        chatRule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        chatRule.eventFilters = List.of(AutomationRule.EventFilter.regex("message", ".*joined the game.*"));
        chatRule.actions = List.of(AutomationRule.Action.macro("announce_join"));
        chatRule.cooldownMs = 2_000L;

        AutomationRule tickRule = AutomationRule.createDefault();
        tickRule.id = "tick_script";
        tickRule.name = "Tick Script";
        tickRule.eventType = AutomationEventType.TICK_INTERVAL;
        tickRule.eventFilters = List.of(intervalFilter(20));
        tickRule.actions = List.of(AutomationRule.Action.script("heartbeat.kts"));
        tickRule.rateLimitCount = 3;
        tickRule.rateLimitWindowMs = 10_000L;

        AutomationRule screenRule = AutomationRule.createDefault();
        screenRule.id = "inventory_screen_ping";
        screenRule.name = "Inventory Screen Ping";
        screenRule.eventType = AutomationEventType.SCREEN_CHANGED;
        screenRule.eventFilters = List.of(equalsFilter("toScreen", "InventoryScreen"));
        screenRule.actions = List.of(AutomationRule.Action.script("heartbeat.kts"));

        AutomationRule weatherRule = AutomationRule.createDefault();
        weatherRule.id = "rain_start_notice";
        weatherRule.name = "Rain Start Notice";
        weatherRule.eventType = AutomationEventType.WEATHER_CHANGED;
        weatherRule.eventFilters = List.of(equalsFilter("startedRaining", "true"));
        weatherRule.actions = List.of(AutomationRule.Action.command("/say weather changed"));

        AutomationRule deathRule = AutomationRule.createDefault();
        deathRule.id = "death_recovery_macro";
        deathRule.name = "Death Recovery Macro";
        deathRule.eventType = AutomationEventType.PLAYER_DEATH;
        deathRule.actions = List.of(AutomationRule.Action.macro("death_recovery"));

        example.rules = List.of(joinRule, chatRule, tickRule, screenRule, weatherRule, deathRule);
        return example;
    }

    private static AutomationRule.EventFilter intervalFilter(int ticks) {
        AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
        filter.field = "intervalTicks";
        filter.operator = AutomationRule.Operator.EQUALS;
        filter.value = Integer.toString(ticks);
        return filter;
    }

    private static AutomationRule.EventFilter equalsFilter(String field, String value) {
        AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
        filter.field = field;
        filter.operator = AutomationRule.Operator.EQUALS;
        filter.value = value;
        return filter;
    }
}
