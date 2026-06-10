package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record AutomationEvent(AutomationEventType type, long timestampMs, long clientTick,
                              Map<String, Object> attributes) {
    public AutomationEvent(@NotNull AutomationEventType type,
                           long timestampMs,
                           long clientTick,
                           @Nullable Map<String, Object> attributes) {
        this.type = Objects.requireNonNull(type);
        this.timestampMs = timestampMs;
        this.clientTick = clientTick;
        this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public @Nullable Object attribute(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        switch (key) {
            case "type", "eventType" -> {
                return type.name();
            }
            case "timestampMs" -> {
                return timestampMs;
            }
            case "clientTick" -> {
                return clientTick;
            }
            default -> {
                if (attributes.containsKey(key)) {
                    return attributes.get(key);
                }
            }
        }

        Object current = attributes;
        for (String part : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public String summary() {
        StringBuilder builder = new StringBuilder(type.name());
        if (!attributes.isEmpty()) {
            builder.append(' ').append(attributes);
        }
        return builder.toString();
    }

    public static AutomationEvent of(@NotNull AutomationEventType type, long timestampMs, long clientTick) {
        return new AutomationEvent(type, timestampMs, clientTick, Map.of());
    }
}
