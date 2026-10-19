package me.m0dii.modules.macros.gui;

import me.m0dii.modules.automation.AutomationEventType;
import me.m0dii.modules.automation.AutomationRule;

import java.util.List;

/**
 * Pure helpers for the automation tab: one-line summaries of rules, filters, conditions and actions, their default
 * values when the type changes, and comparisons used to decide when to reset them.
 */
public final class MacroWorkbenchAutomationSupport {
    private MacroWorkbenchAutomationSupport() {
    }

    public static int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    public static int clampIndex(int current, int size) {
        if (size <= 0) {
            return -1;
        }
        return Math.clamp(Math.max(current, 0), 0, size - 1);
    }

    public static String displayRuleName(AutomationRule rule) {
        String name = safe(rule.name);
        String label = name.isBlank() ? safe(rule.id) : name;
        String folder = safe(rule.group);
        return folder.isBlank() ? label : "[" + folder + "] " + label;
    }

    public static String filterSummary(AutomationRule.EventFilter filter) {
        if (filter == null) {
            return "(none)";
        }
        return safe(filter.field) + " " + filter.operator.name() + " " + safe(filter.value);
    }

    public static String conditionSummary(AutomationRule.Condition condition) {
        if (condition == null) {
            return "(none)";
        }
        return condition.source.name() + "." + displayConditionField(condition.source, condition.field) + " " + condition.operator.name() + " " + safe(condition.value);
    }

    public static String actionSummary(AutomationRule.Action action) {
        if (action == null) {
            return "(none)";
        }
        String prefix = action.enabled ? "" : "[off] ";
        String target = safe(action.target);
        return prefix + action.type.name() + " -> " + (target.isBlank() ? "(unset)" : target);
    }

    public static String actionHint(AutomationRule rule) {
        if (rule == null) {
            return "Select a rule to edit triggers, conditions, and actions.";
        }
        return switch (rule.eventType) {
            case TICK_INTERVAL -> "Tick rules use an event filter like field=intervalTicks value=20.";
            case PLAYER_MOVE -> "Move rules can filter on distance or destination coordinates.";
            case WORLD_JOIN -> "Join rules usually need no filters.";
            case WORLD_LEAVE -> "Leave rules trigger when unloading/disconnecting from a world.";
            case DIMENSION_CHANGE -> "Dimension rules often use toDimension or fromDimension.";
            case CHAT_RECEIVED_REGEX -> "Chat rules usually use field=message with REGEX.";
            case SCREEN_CHANGED -> "Screen rules can match fromScreen, toScreen, or screen presence changes.";
            case WEATHER_CHANGED -> "Weather rules can detect rain/thunder starts and stops.";
            case HOTBAR_SLOT_CHANGED -> "Hotbar rules can match fromSlot or toSlot.";
            case HELD_ITEM_CHANGED -> "Held-item rules can match item ids or display names.";
            case PLAYER_LEVEL_CHANGED -> "Level rules can react to level-ups or XP progress changes.";
            case PLAYER_HEALTH_CHANGED -> "Health rules can match toHealth or deltaHealth.";
            case PLAYER_FOOD_CHANGED -> "Food rules can match toFood or deltaFood.";
            case PLAYER_DEATH -> "Death rules run once when the client player dies.";
        };
    }

    public static AutomationRule.EventFilter defaultFilterFor(AutomationEventType eventType) {
        AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
        filter.ignoreCase = true;
        switch (eventType) {
            case TICK_INTERVAL -> {
                filter.field = "intervalTicks";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "20";
            }
            case PLAYER_MOVE -> {
                filter.field = "distance";
                filter.operator = AutomationRule.Operator.GREATER_THAN;
                filter.value = "1.0";
            }
            case WORLD_JOIN -> {
                filter.field = "dimension";
                filter.operator = AutomationRule.Operator.EXISTS;
                filter.value = "";
            }
            case WORLD_LEAVE -> {
                filter.field = "fromDimension";
                filter.operator = AutomationRule.Operator.EXISTS;
                filter.value = "";
            }
            case DIMENSION_CHANGE -> {
                filter.field = "toDimension";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "minecraft:the_nether";
            }
            case CHAT_RECEIVED_REGEX -> {
                filter.field = "message";
                filter.operator = AutomationRule.Operator.REGEX;
                filter.value = ".*";
            }
            case SCREEN_CHANGED -> {
                filter.field = "toScreen";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "InventoryScreen";
            }
            case WEATHER_CHANGED -> {
                filter.field = "startedRaining";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "true";
            }
            case HOTBAR_SLOT_CHANGED -> {
                filter.field = "toSlot";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "1";
            }
            case HELD_ITEM_CHANGED -> {
                filter.field = "toItemId";
                filter.operator = AutomationRule.Operator.EQUALS;
                filter.value = "minecraft:diamond_sword";
            }
            case PLAYER_LEVEL_CHANGED -> {
                filter.field = "deltaLevel";
                filter.operator = AutomationRule.Operator.GREATER_THAN;
                filter.value = "0";
            }
            case PLAYER_HEALTH_CHANGED -> {
                filter.field = "deltaHealth";
                filter.operator = AutomationRule.Operator.LESS_THAN;
                filter.value = "0";
            }
            case PLAYER_FOOD_CHANGED -> {
                filter.field = "deltaFood";
                filter.operator = AutomationRule.Operator.LESS_THAN;
                filter.value = "0";
            }
            case PLAYER_DEATH -> {
                filter.field = "health";
                filter.operator = AutomationRule.Operator.LESS_OR_EQUAL;
                filter.value = "0";
            }
        }
        return filter;
    }

    public static AutomationRule.Condition defaultConditionFor(AutomationEventType eventType) {
        AutomationRule.Condition condition = new AutomationRule.Condition();
        condition.source = AutomationRule.ConditionSource.EVENT;
        condition.operator = AutomationRule.Operator.EQUALS;
        condition.ignoreCase = true;
        switch (eventType) {
            case TICK_INTERVAL -> condition.field = "worldLoaded";
            case PLAYER_MOVE -> condition.field = "distance";
            case WORLD_JOIN -> condition.field = "dimension";
            case WORLD_LEAVE -> condition.field = "fromDimension";
            case DIMENSION_CHANGE -> condition.field = "toDimension";
            case CHAT_RECEIVED_REGEX -> {
                condition.field = "message";
                condition.operator = AutomationRule.Operator.CONTAINS;
            }
            case SCREEN_CHANGED -> condition.field = "toScreen";
            case WEATHER_CHANGED -> condition.field = "startedRaining";
            case HOTBAR_SLOT_CHANGED -> condition.field = "toSlot";
            case HELD_ITEM_CHANGED -> condition.field = "toItemId";
            case PLAYER_LEVEL_CHANGED -> condition.field = "toLevel";
            case PLAYER_HEALTH_CHANGED -> condition.field = "toHealth";
            case PLAYER_FOOD_CHANGED -> condition.field = "toFood";
            case PLAYER_DEATH -> condition.field = "health";
        }
        condition.value = "";
        return condition;
    }

    public static AutomationRule.Action defaultActionFor(AutomationEventType eventType) {
        return AutomationRule.Action.script("heartbeat.kts");
    }

    public static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String sanitizeInlineText(String value) {
        return safe(value).replace('\n', ' ').replace('\r', ' ');
    }

    public static <E extends Enum<E>> E nextEnum(E current) {
        E[] values = current.getDeclaringClass().getEnumConstants();
        return values[(current.ordinal() + 1) % values.length];
    }

    public static String normalizePlaceholderField(String field) {
        String value = safe(field).trim();
        if (value.startsWith("{") && value.endsWith("}") && value.length() > 2) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    public static String displayConditionField(AutomationRule.ConditionSource source, String field) {
        String normalized = normalizePlaceholderField(field);
        if (source == AutomationRule.ConditionSource.PLACEHOLDER) {
            return formatPlaceholderKey(normalized);
        }
        return normalized;
    }

    public static String formatPlaceholderKey(String key) {
        String normalized = normalizePlaceholderField(key);
        return normalized.isBlank() ? "{}" : "{" + normalized + "}";
    }

    public static boolean shouldResetFilters(AutomationRule rule, AutomationEventType previousType) {
        if (rule.eventFilters == null || rule.eventFilters.isEmpty()) {
            return true;
        }
        if (rule.eventFilters.size() != 1) {
            return false;
        }
        return sameFilter(rule.eventFilters.getFirst(), defaultFilterFor(previousType));
    }

    public static boolean shouldResetActions(AutomationRule rule, AutomationEventType previousType) {
        if (rule.actions == null || rule.actions.isEmpty()) {
            return true;
        }
        if (rule.actions.size() != 1) {
            return false;
        }
        return sameAction(rule.actions.getFirst(), defaultActionFor(previousType));
    }

    private static boolean sameFilter(AutomationRule.EventFilter left, AutomationRule.EventFilter right) {
        return left != null
                && right != null
                && safe(left.field).equals(safe(right.field))
                && left.operator == right.operator
                && safe(left.value).equals(safe(right.value))
                && left.ignoreCase == right.ignoreCase;
    }

    private static boolean sameAction(AutomationRule.Action left, AutomationRule.Action right) {
        return left != null
                && right != null
                && left.type == right.type
                && safe(left.target).equals(safe(right.target))
                && safe(left.argument).equals(safe(right.argument))
                && left.enabled == right.enabled;
    }
}
