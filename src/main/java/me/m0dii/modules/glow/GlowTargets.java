package me.m0dii.modules.glow;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What {@code /glow} outlines: entity type ids ("minecraft:zombie") or groups ("hostile", "passive", "items",
 * "players"), each with an outline colour. Pure Java; the mixins ask {@link #colorFor} per entity.
 */
public final class GlowTargets {
    public static final String[] GROUPS = {"hostile", "passive", "items", "players", "all"};
    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("white", 0xFFFFFF);
        COLORS.put("red", 0xFF5555);
        COLORS.put("orange", 0xFFAA00);
        COLORS.put("yellow", 0xFFFF55);
        COLORS.put("green", 0x55FF55);
        COLORS.put("aqua", 0x55FFFF);
        COLORS.put("blue", 0x5555FF);
        COLORS.put("purple", 0xAA00AA);
        COLORS.put("pink", 0xFF55FF);
        COLORS.put("gray", 0xAAAAAA);
    }

    private final Map<String, Integer> targets = new LinkedHashMap<>();

    public static Map<String, Integer> colors() {
        return COLORS;
    }

    /** Colour by name or #rrggbb; null when unknown. */
    public static Integer parseColor(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (COLORS.containsKey(value)) {
            return COLORS.get(value);
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.matches("[0-9a-f]{6}")) {
            return Integer.parseInt(hex, 16);
        }
        return null;
    }

    /** Normalises "zombie" to "minecraft:zombie"; groups stay as they are. */
    public static String normalize(String target) {
        String value = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
        for (String group : GROUPS) {
            if (group.equals(value)) {
                return value;
            }
        }
        return value.contains(":") ? value : "minecraft:" + value;
    }

    /** Adds the target, or removes it if it was already glowing with the same colour. Returns true if now glowing. */
    public synchronized boolean toggle(String target, Integer color) {
        String key = normalize(target);
        int resolved = color == null ? defaultColor(key) : color;
        Integer previous = targets.get(key);
        if (previous != null && (color == null || previous == resolved)) {
            targets.remove(key);
            return false;
        }
        targets.put(key, resolved);
        return true;
    }

    public synchronized void clear() {
        targets.clear();
    }

    public synchronized boolean isEmpty() {
        return targets.isEmpty();
    }

    public synchronized Map<String, Integer> snapshot() {
        return Map.copyOf(targets);
    }

    /**
     * Outline colour for an entity, or null when it should not glow. Exact types win over groups.
     *
     * @param typeId  entity type id, e.g. "minecraft:zombie"
     * @param groups  the groups the entity belongs to
     */
    public synchronized Integer colorFor(String typeId, String... groups) {
        if (targets.isEmpty()) {
            return null;
        }
        Integer exact = targets.get(typeId);
        if (exact != null) {
            return exact;
        }
        for (String group : groups) {
            Integer color = targets.get(group);
            if (color != null) {
                return color;
            }
        }
        return targets.get("all");
    }

    static int defaultColor(String key) {
        return switch (key) {
            case "hostile" -> 0xFF5555;
            case "passive" -> 0x55FF55;
            case "items" -> 0xFFFF55;
            case "players" -> 0x55FFFF;
            default -> 0xFFFFFF;
        };
    }
}
