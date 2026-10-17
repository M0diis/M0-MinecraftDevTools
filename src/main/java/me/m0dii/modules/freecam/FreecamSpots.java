package me.m0dii.modules.freecam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.*;

/** Saved freecam camera spots: nine slots per world (server or singleplayer save). */
public final class FreecamSpots {
    public static final int SLOTS = 9;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record Spot(String dimension, double x, double y, double z, float yaw, float pitch) {
        public String describe() {
            return String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f (%s)", x, y, z, shortDimension(dimension));
        }
    }

    private final Map<String, Map<Integer, Spot>> byWorld = new LinkedHashMap<>();

    public void set(String world, int slot, Spot spot) {
        checkSlot(slot);
        byWorld.computeIfAbsent(world, k -> new TreeMap<>()).put(slot, spot);
    }

    public Spot get(String world, int slot) {
        Map<Integer, Spot> spots = byWorld.get(world);
        return spots == null ? null : spots.get(slot);
    }

    public boolean clear(String world, int slot) {
        Map<Integer, Spot> spots = byWorld.get(world);
        return spots != null && spots.remove(slot) != null;
    }

    /** Used slot numbers in order. */
    public List<Integer> used(String world) {
        Map<Integer, Spot> spots = byWorld.get(world);
        return spots == null ? List.of() : new ArrayList<>(spots.keySet());
    }

    /** First empty slot, or -1 when all nine are used. */
    public int firstFree(String world) {
        for (int slot = 1; slot <= SLOTS; slot++) {
            if (get(world, slot) == null) {
                return slot;
            }
        }
        return -1;
    }

    /** The used slot after {@code current} (wrapping), or -1 when there are none. */
    public int next(String world, int current) {
        List<Integer> used = used(world);
        if (used.isEmpty()) {
            return -1;
        }
        for (int slot : used) {
            if (slot > current) {
                return slot;
            }
        }
        return used.get(0);
    }

    public String toJson() {
        Map<String, Map<String, Spot>> out = new LinkedHashMap<>();
        byWorld.forEach((world, spots) -> {
            if (!spots.isEmpty()) {
                Map<String, Spot> slots = new LinkedHashMap<>();
                spots.forEach((slot, spot) -> slots.put(Integer.toString(slot), spot));
                out.put(world, slots);
            }
        });
        return GSON.toJson(out);
    }

    public static FreecamSpots fromJson(String json) {
        FreecamSpots spots = new FreecamSpots();
        Map<String, Map<String, Spot>> raw = GSON.fromJson(json, new TypeToken<Map<String, Map<String, Spot>>>() {
        }.getType());
        if (raw == null) {
            return spots;
        }
        raw.forEach((world, slots) -> {
            if (world == null || slots == null) {
                return;
            }
            slots.forEach((slotText, spot) -> {
                try {
                    int slot = Integer.parseInt(slotText.trim());
                    if (slot >= 1 && slot <= SLOTS && spot != null && spot.dimension() != null
                            && Double.isFinite(spot.x()) && Double.isFinite(spot.y()) && Double.isFinite(spot.z())) {
                        spots.set(world, slot, spot);
                    }
                } catch (NumberFormatException ignored) {
                    // skip junk slots
                }
            });
        });
        return spots;
    }

    private static void checkSlot(int slot) {
        if (slot < 1 || slot > SLOTS) {
            throw new IllegalArgumentException("Slot must be 1-" + SLOTS);
        }
    }

    static String shortDimension(String dimension) {
        if (dimension == null) {
            return "?";
        }
        return dimension.startsWith("minecraft:") ? dimension.substring("minecraft:".length()) : dimension;
    }
}
