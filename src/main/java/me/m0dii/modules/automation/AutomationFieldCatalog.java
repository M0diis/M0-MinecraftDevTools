package me.m0dii.modules.automation;

import me.m0dii.modules.macros.MacroPlaceholders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AutomationFieldCatalog {
    public record FieldOption(String key, String label, String description) {
    }

    private static final List<FieldOption> EVENT_META_FIELDS = List.of(
            field("type", "Event Type", "The automation event type name."),
            field("timestampMs", "Timestamp", "Unix timestamp in milliseconds."),
            field("clientTick", "Client Tick", "Client tick counter when the event fired.")
    );

    private static final Map<AutomationEventType, List<FieldOption>> EVENT_FILTER_FIELDS = new EnumMap<>(AutomationEventType.class);
    private static final Map<AutomationEventType, List<FieldOption>> EVENT_CONDITION_FIELDS = new EnumMap<>(AutomationEventType.class);
    private static final Map<AutomationRule.ConditionSource, List<FieldOption>> SNAPSHOT_FIELDS = new EnumMap<>(AutomationRule.ConditionSource.class);

    static {
        EVENT_FILTER_FIELDS.put(AutomationEventType.TICK_INTERVAL, join(EVENT_META_FIELDS, List.of(
                field("intervalTicks", "Tick Interval", "Virtual helper that matches every N client ticks."),
                field("tick", "Tick Counter", "Current client tick counter."),
                field("worldLoaded", "World Loaded", "True when a world is loaded."),
                field("playerLoaded", "Player Loaded", "True when the player entity exists."),
                field("screen", "Screen", "Current screen class name."),
                field("screenPresent", "Screen Present", "True when any screen is open."),
                field("dimension", "Dimension", "Current world dimension id."),
                field("worldTime", "World Time", "Current world time.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.PLAYER_MOVE, join(EVENT_META_FIELDS, List.of(
                field("fromX", "From X", "Previous player X position."),
                field("fromY", "From Y", "Previous player Y position."),
                field("fromZ", "From Z", "Previous player Z position."),
                field("toX", "To X", "Current player X position."),
                field("toY", "To Y", "Current player Y position."),
                field("toZ", "To Z", "Current player Z position."),
                field("deltaX", "Delta X", "Movement delta on the X axis."),
                field("deltaY", "Delta Y", "Movement delta on the Y axis."),
                field("deltaZ", "Delta Z", "Movement delta on the Z axis."),
                field("distance", "Distance", "Total movement distance for this event.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.WORLD_JOIN, join(EVENT_META_FIELDS, List.of(
                field("dimension", "Dimension", "Dimension id for the joined world."),
                field("playerName", "Player Name", "Client player display name.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.DIMENSION_CHANGE, join(EVENT_META_FIELDS, List.of(
                field("fromDimension", "From Dimension", "Previous dimension id."),
                field("toDimension", "To Dimension", "New dimension id.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.CHAT_RECEIVED_REGEX, join(EVENT_META_FIELDS, List.of(
                field("message", "Message", "Rendered chat message text."),
                field("messageLength", "Message Length", "Rendered chat message length.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.SCREEN_CHANGED, join(EVENT_META_FIELDS, List.of(
                field("fromScreen", "From Screen", "Previous screen class name."),
                field("toScreen", "To Screen", "New screen class name."),
                field("fromScreenPresent", "From Screen Present", "True when a screen was open before the change."),
                field("toScreenPresent", "To Screen Present", "True when a screen is open after the change.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.HOTBAR_SLOT_CHANGED, join(EVENT_META_FIELDS, List.of(
                field("fromSlot", "From Slot", "Previous hotbar slot index."),
                field("toSlot", "To Slot", "New hotbar slot index.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.HELD_ITEM_CHANGED, join(EVENT_META_FIELDS, List.of(
                field("fromItemId", "From Item Id", "Previous main-hand item id."),
                field("toItemId", "To Item Id", "New main-hand item id."),
                field("fromItemName", "From Item Name", "Previous main-hand item display name."),
                field("toItemName", "To Item Name", "New main-hand item display name.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.PLAYER_HEALTH_CHANGED, join(EVENT_META_FIELDS, List.of(
                field("fromHealth", "From Health", "Previous player health."),
                field("toHealth", "To Health", "New player health."),
                field("deltaHealth", "Delta Health", "Health change amount.")
        )));
        EVENT_FILTER_FIELDS.put(AutomationEventType.PLAYER_FOOD_CHANGED, join(EVENT_META_FIELDS, List.of(
                field("fromFood", "From Food", "Previous hunger level."),
                field("toFood", "To Food", "New hunger level."),
                field("deltaFood", "Delta Food", "Food level change amount.")
        )));

        EVENT_CONDITION_FIELDS.put(AutomationEventType.TICK_INTERVAL, join(EVENT_META_FIELDS, List.of(
                field("tick", "Tick Counter", "Current client tick counter."),
                field("worldLoaded", "World Loaded", "True when a world is loaded."),
                field("playerLoaded", "Player Loaded", "True when the player entity exists."),
                field("screen", "Screen", "Current screen class name."),
                field("screenPresent", "Screen Present", "True when any screen is open."),
                field("dimension", "Dimension", "Current world dimension id."),
                field("worldTime", "World Time", "Current world time.")
        )));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.PLAYER_MOVE, EVENT_FILTER_FIELDS.get(AutomationEventType.PLAYER_MOVE));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.WORLD_JOIN, EVENT_FILTER_FIELDS.get(AutomationEventType.WORLD_JOIN));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.DIMENSION_CHANGE, EVENT_FILTER_FIELDS.get(AutomationEventType.DIMENSION_CHANGE));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.CHAT_RECEIVED_REGEX, EVENT_FILTER_FIELDS.get(AutomationEventType.CHAT_RECEIVED_REGEX));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.SCREEN_CHANGED, EVENT_FILTER_FIELDS.get(AutomationEventType.SCREEN_CHANGED));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.HOTBAR_SLOT_CHANGED, EVENT_FILTER_FIELDS.get(AutomationEventType.HOTBAR_SLOT_CHANGED));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.HELD_ITEM_CHANGED, EVENT_FILTER_FIELDS.get(AutomationEventType.HELD_ITEM_CHANGED));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.PLAYER_HEALTH_CHANGED, EVENT_FILTER_FIELDS.get(AutomationEventType.PLAYER_HEALTH_CHANGED));
        EVENT_CONDITION_FIELDS.put(AutomationEventType.PLAYER_FOOD_CHANGED, EVENT_FILTER_FIELDS.get(AutomationEventType.PLAYER_FOOD_CHANGED));

        SNAPSHOT_FIELDS.put(AutomationRule.ConditionSource.CLIENT, List.of(
                field("connected", "Connected", "True when both player and world exist."),
                field("screen", "Screen", "Current screen class name."),
                field("screenPresent", "Screen Present", "True when any screen is open."),
                field("paused", "Paused", "True when the client is paused."),
                field("worldLoaded", "World Loaded", "True when a world is loaded."),
                field("playerLoaded", "Player Loaded", "True when the player entity exists.")
        ));
        SNAPSHOT_FIELDS.put(AutomationRule.ConditionSource.PLAYER, List.of(
                field("name", "Name", "Player display name."),
                field("x", "X", "Current player X position."),
                field("y", "Y", "Current player Y position."),
                field("z", "Z", "Current player Z position."),
                field("health", "Health", "Current player health."),
                field("maxHealth", "Max Health", "Player max health."),
                field("food", "Food", "Current hunger level."),
                field("sneaking", "Sneaking", "True when the player is sneaking."),
                field("sprinting", "Sprinting", "True when the player is sprinting."),
                field("dimension", "Dimension", "Current dimension id."),
                field("hotbarSlot", "Hotbar Slot", "Current selected hotbar slot."),
                field("mainHandItemId", "Main Hand Item Id", "Current main-hand item id."),
                field("mainHandItemName", "Main Hand Item Name", "Current main-hand item display name.")
        ));
        SNAPSHOT_FIELDS.put(AutomationRule.ConditionSource.WORLD, List.of(
                field("dimension", "Dimension", "Current dimension id."),
                field("time", "Time", "Current world time."),
                field("raining", "Raining", "True when it is raining."),
                field("thundering", "Thundering", "True when it is thundering.")
        ));
    }

    private AutomationFieldCatalog() {
    }

    public static @NotNull List<FieldOption> eventFilterFields(@Nullable AutomationEventType eventType) {
        if (eventType == null) {
            return List.of();
        }
        return EVENT_FILTER_FIELDS.getOrDefault(eventType, List.of());
    }

    public static @NotNull List<FieldOption> conditionFields(@Nullable AutomationRule.ConditionSource source,
                                                             @Nullable AutomationEventType eventType) {
        if (source == null) {
            return List.of();
        }
        if (source == AutomationRule.ConditionSource.EVENT) {
            return eventType == null ? EVENT_META_FIELDS : EVENT_CONDITION_FIELDS.getOrDefault(eventType, EVENT_META_FIELDS);
        }
        if (source == AutomationRule.ConditionSource.PLACEHOLDER) {
            return placeholderFields();
        }
        return SNAPSHOT_FIELDS.getOrDefault(source, List.of());
    }

    public static @Nullable FieldOption findEventFilterField(@Nullable AutomationEventType eventType, @Nullable String key) {
        return find(eventFilterFields(eventType), key);
    }

    public static @Nullable FieldOption findConditionField(@Nullable AutomationRule.ConditionSource source,
                                                           @Nullable AutomationEventType eventType,
                                                           @Nullable String key) {
        return find(conditionFields(source, eventType), key);
    }

    private static @Nullable FieldOption find(List<FieldOption> options, @Nullable String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalizedKey = normalizeKey(key);
        for (FieldOption option : options) {
            if (Objects.equals(option.key(), normalizedKey)) {
                return option;
            }
        }
        return null;
    }

    private static @NotNull FieldOption field(String key, String label, String description) {
        return new FieldOption(key, label, description);
    }

    private static String normalizeKey(String raw) {
        String value = raw.trim();
        if (value.startsWith("{") && value.endsWith("}") && value.length() > 2) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static @NotNull List<FieldOption> placeholderFields() {
        List<String> tokens = MacroPlaceholders.getKnownPlaceholderTokens();
        java.util.ArrayList<FieldOption> options = new java.util.ArrayList<>(tokens.size());
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            String cleaned = token.trim();
            options.add(field(cleaned, cleaned, "Macro placeholder token, evaluated as {" + cleaned + "} against current client state."));
        }
        return List.copyOf(options);
    }

    private static @NotNull List<FieldOption> join(List<FieldOption> left, List<FieldOption> right) {
        java.util.ArrayList<FieldOption> joined = new java.util.ArrayList<>(left.size() + right.size());
        joined.addAll(left);
        joined.addAll(right);
        return List.copyOf(joined);
    }
}
