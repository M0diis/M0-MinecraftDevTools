package me.m0dii.modules.automation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AutomationEvent {
    private final AutomationEventType type;
    private final long timestampMs;
    private final long clientTick;
    private final Map<String, Object> attributes;

    public AutomationEvent(@NotNull AutomationEventType type,
                           long timestampMs,
                           long clientTick,
                           @Nullable Map<String, Object> attributes) {
        this.type = Objects.requireNonNull(type);
        this.timestampMs = timestampMs;
        this.clientTick = clientTick;
        this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public AutomationEventType type() {
        return type;
    }

    public long timestampMs() {
        return timestampMs;
    }

    public long clientTick() {
        return clientTick;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public @Nullable Object attribute(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.equals("type") || key.equals("eventType")) {
            return type.name();
        }
        if (key.equals("timestampMs")) {
            return timestampMs;
        }
        if (key.equals("clientTick")) {
            return clientTick;
        }
        if (attributes.containsKey(key)) {
            return attributes.get(key);
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
