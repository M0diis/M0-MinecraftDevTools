package me.m0dii.modules.getdata;

import java.util.*;

/** Builds the commands the quick editor sends: only what changed, as {@code /data merge} and {@code /data remove}. */
public final class QuickEditCommands {
    /** Entity flags shown as toggles, in display order, with the NBT key they write. */
    public static final List<String> ENTITY_FLAGS = List.of(
            "Glowing", "NoGravity", "Silent", "Invulnerable", "NoAI", "PersistenceRequired", "CustomNameVisible");

    /** What the quick editor shows for an entity. {@code health} is null for non-living entities. */
    public record EntityValues(String name, String health, Map<String, Boolean> flags) {
        public EntityValues {
            name = name == null ? "" : name;
            flags = new LinkedHashMap<>(flags == null ? Map.of() : flags);
        }
    }

    private QuickEditCommands() {
    }

    /**
     * Commands (without the leading slash) turning {@code before} into {@code after} for {@code targetToken}
     * (e.g. {@code entity 0f3c...}). Throws {@link IllegalArgumentException} with a readable message for bad input.
     */
    public static List<String> entity(String targetToken, EntityValues before, EntityValues after) {
        StringBuilder merge = new StringBuilder();
        List<String> commands = new ArrayList<>();

        String newName = after.name().strip();
        if (!newName.equals(before.name().strip())) {
            if (newName.isEmpty()) {
                commands.add("data remove " + targetToken + " CustomName");
            } else {
                append(merge, "CustomName", quote(newName));
            }
        }

        if (after.health() != null && !after.health().strip().equals(before.health() == null ? "" : before.health().strip())) {
            float health;
            try {
                health = Float.parseFloat(after.health().strip());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Health must be a number, e.g. 20");
            }
            if (!(health > 0) || Float.isInfinite(health)) {
                throw new IllegalArgumentException("Health must be above 0 (use /kill to remove an entity)");
            }
            append(merge, "Health", trimFloat(health) + "f");
        }

        for (String flag : ENTITY_FLAGS) {
            boolean was = Boolean.TRUE.equals(before.flags().get(flag));
            Boolean now = after.flags().get(flag);
            if (now != null && now != was) {
                append(merge, flag, now ? "1b" : "0b");
            }
        }

        if (!merge.isEmpty()) {
            commands.add(0, "data merge " + targetToken + " {" + merge + "}");
        }
        return commands;
    }

    /** {@code [facing=north,half=top]} from property values, or "" when there are none. */
    public static String stateSuffix(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("[");
        properties.forEach((key, value) -> {
            if (out.length() > 1) {
                out.append(',');
            }
            out.append(key).append('=').append(value);
        });
        return out.append(']').toString();
    }

    /** Next (or previous) value in {@code values} after {@code current}, wrapping around. */
    public static String cycle(List<String> values, String current, int direction) {
        if (values == null || values.isEmpty()) {
            return current;
        }
        int index = values.indexOf(current);
        int next = Math.floorMod((index < 0 ? 0 : index + (direction < 0 ? -1 : 1)), values.size());
        return values.get(next);
    }

    static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String trimFloat(float value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e7) {
            return Integer.toString((int) value) + ".0";
        }
        return String.format(Locale.ROOT, "%s", value);
    }

    private static void append(StringBuilder merge, String key, String value) {
        if (!merge.isEmpty()) {
            merge.append(',');
        }
        merge.append(key).append(':').append(value);
    }
}
