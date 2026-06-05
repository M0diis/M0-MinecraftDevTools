package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class AutomationRule {
    public String id = newId();
    public String name = "New Rule";
    public boolean enabled = true;
    public AutomationEventType eventType = AutomationEventType.TICK_INTERVAL;
    public List<EventFilter> eventFilters = new ArrayList<>();
    public List<Condition> conditions = new ArrayList<>();
    public List<Action> actions = new ArrayList<>();
    public long cooldownMs = 0L;
    public long debounceMs = 0L;
    public int rateLimitCount = 0;
    public long rateLimitWindowMs = 0L;
    public int priority = 0;

    public static String newId() {
        return "rule_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static AutomationRule createDefault() {
        AutomationRule rule = new AutomationRule();
        rule.actions.add(Action.command("/say automation rule fired"));
        return rule;
    }

    public AutomationRule copy() {
        AutomationRule copy = new AutomationRule();
        copy.id = id;
        copy.name = name;
        copy.enabled = enabled;
        copy.eventType = eventType;
        copy.cooldownMs = cooldownMs;
        copy.debounceMs = debounceMs;
        copy.rateLimitCount = rateLimitCount;
        copy.rateLimitWindowMs = rateLimitWindowMs;
        copy.priority = priority;
        copy.eventFilters = eventFilters == null ? new ArrayList<>() : eventFilters.stream().map(EventFilter::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.conditions = conditions == null ? new ArrayList<>() : conditions.stream().map(Condition::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.actions = actions == null ? new ArrayList<>() : actions.stream().map(Action::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return copy;
    }

    public String displayName() {
        return (name == null || name.isBlank()) ? id : name;
    }

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        REGEX,
        GREATER_THAN,
        GREATER_OR_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL,
        EXISTS,
        NOT_EXISTS,
        IN_LIST
    }

    public enum ConditionSource {
        EVENT,
        CLIENT,
        PLAYER,
        WORLD,
        PLACEHOLDER
    }

    public static final class EventFilter {
        public String field = "";
        public Operator operator = Operator.EQUALS;
        public String value = "";
        public boolean ignoreCase = true;

        public EventFilter copy() {
            EventFilter copy = new EventFilter();
            copy.field = field;
            copy.operator = operator;
            copy.value = value;
            copy.ignoreCase = ignoreCase;
            return copy;
        }

        public static EventFilter regex(@NotNull String field, @NotNull String pattern) {
            EventFilter filter = new EventFilter();
            filter.field = field;
            filter.operator = Operator.REGEX;
            filter.value = pattern;
            return filter;
        }
    }

    public static final class Condition {
        public ConditionSource source = ConditionSource.EVENT;
        public String field = "";
        public Operator operator = Operator.EQUALS;
        public String value = "";
        public boolean ignoreCase = true;

        public Condition copy() {
            Condition copy = new Condition();
            copy.source = source;
            copy.field = field;
            copy.operator = operator;
            copy.value = value;
            copy.ignoreCase = ignoreCase;
            return copy;
        }
    }

    public static final class Action {
        public AutomationActionType type = AutomationActionType.SEND_CLIENT_COMMAND;
        public String target = "";
        public String argument = "";
        public boolean enabled = true;
        public Map<String, String> parameters = new LinkedHashMap<>();

        public Action copy() {
            Action copy = new Action();
            copy.type = type;
            copy.target = target;
            copy.argument = argument;
            copy.enabled = enabled;
            copy.parameters = parameters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parameters);
            return copy;
        }

        public static Action command(@NotNull String command) {
            Action action = new Action();
            action.type = AutomationActionType.SEND_CLIENT_COMMAND;
            action.target = command;
            return action;
        }

        public static Action macro(@NotNull String macroId) {
            Action action = new Action();
            action.type = AutomationActionType.RUN_MACRO;
            action.target = macroId;
            return action;
        }

        public static Action script(@NotNull String file) {
            Action action = new Action();
            action.type = AutomationActionType.RUN_SCRIPT;
            action.target = file;
            return action;
        }

        public static Action toggleModule(@NotNull String moduleId) {
            Action action = new Action();
            action.type = AutomationActionType.TOGGLE_MODULE;
            action.target = moduleId;
            return action;
        }

        public String summary() {
            String base = (type == null ? AutomationActionType.SEND_CLIENT_COMMAND : type).name() + ":" + (target == null ? "" : target.trim());
            if (argument == null || argument.isBlank()) {
                return base;
            }
            return base + " (" + argument.trim() + ")";
        }
    }

    public static Operator parseOperator(String raw, Operator fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Operator.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
