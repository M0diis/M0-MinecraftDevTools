package me.m0dii.modules.mobdrops;

import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class MobDropTrackerClientState {
    public enum OverlayMode {
        OFF,
        ALL,
        SELECTED;

        private static OverlayMode fromToken(String token) {
            return switch (token == null ? "" : token.trim().toLowerCase(Locale.ROOT)) {
                case "all" -> ALL;
                case "selected" -> SELECTED;
                default -> OFF;
            };
        }
    }

    public record TrackerEntry(String name,
                               String kind,
                               String dimensionId,
                               BlockPos anchor,
                               BlockPos min,
                               BlockPos max,
                               long totalItems,
                               long stackCount,
                               long killCount,
                               long dropsPerMinute,
                               String lastMobType,
                               Map<String, Long> itemCounts) {
        public List<Map.Entry<String, Long>> sortedItems() {
            return itemCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                            .thenComparing(Map.Entry.comparingByKey()))
                    .toList();
        }
    }

    private static volatile List<TrackerEntry> trackers = List.of();
    private static volatile OverlayMode overlayMode = OverlayMode.OFF;
    private static volatile Set<String> overlayNames = Set.of();

    private MobDropTrackerClientState() {
    }

    public static void apply(MobDropTrackerPayloads.StatePayload payload) {
        List<TrackerEntry> updated = new ArrayList<>(payload.trackers().size());
        for (MobDropTrackerPayloads.TrackerPayload tracker : payload.trackers()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (MobDropTrackerPayloads.ItemCountEntry item : tracker.itemCounts()) {
                counts.put(item.itemId(), item.count());
            }
            updated.add(new TrackerEntry(
                    tracker.name(),
                    tracker.kind(),
                    tracker.dimensionId(),
                    tracker.anchor(),
                    tracker.min(),
                    tracker.max(),
                    tracker.totalItems(),
                    tracker.stackCount(),
                    tracker.killCount(),
                    tracker.dropsPerMinute(),
                    tracker.lastMobType(),
                    Map.copyOf(counts)
            ));
        }

        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String name : payload.overlayNames()) {
            if (name != null && !name.isBlank()) {
                names.add(name.trim().toLowerCase(Locale.ROOT));
            }
        }

        trackers = List.copyOf(updated);
        overlayMode = OverlayMode.fromToken(payload.overlayMode());
        overlayNames = Set.copyOf(names);
    }

    public static void clear() {
        trackers = List.of();
        overlayMode = OverlayMode.OFF;
        overlayNames = Set.of();
    }

    public static List<TrackerEntry> visibleTrackers(String dimensionId) {
        if (overlayMode == OverlayMode.OFF) {
            return List.of();
        }

        List<TrackerEntry> visible = new ArrayList<>();
        for (TrackerEntry tracker : trackers) {
            if (!tracker.dimensionId().equals(dimensionId)) {
                continue;
            }
            if (overlayMode == OverlayMode.ALL || overlayNames.contains(normalizeName(tracker.name()))) {
                visible.add(tracker);
            }
        }
        return visible;
    }

    public static List<String> trackerNames() {
        return trackers.stream()
                .map(TrackerEntry::name)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static @Nullable TrackerEntry getTracker(String rawName) {
        String normalized = normalizeName(rawName);
        for (TrackerEntry tracker : trackers) {
            if (normalizeName(tracker.name()).equals(normalized)) {
                return tracker;
            }
        }
        return null;
    }

    public static String itemSummary(String rawName, int limit) {
        TrackerEntry tracker = getTracker(rawName);
        if (tracker == null || tracker.itemCounts().isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Long> entry : tracker.sortedItems()) {
            parts.add(compactItemId(entry.getKey()) + " x" + entry.getValue());
            count++;
            if (count >= limit) {
                break;
            }
        }
        return String.join(", ", parts);
    }

    public static String topSummary(String rawName) {
        TrackerEntry tracker = getTracker(rawName);
        if (tracker == null || tracker.itemCounts().isEmpty()) {
            return "";
        }
        Map.Entry<String, Long> top = tracker.sortedItems().getFirst();
        return compactItemId(top.getKey()) + " x" + top.getValue();
    }

    public static String totalCount(String rawName) {
        TrackerEntry tracker = getTracker(rawName);
        return tracker == null ? "0" : Long.toString(tracker.totalItems());
    }

    public static String killCount(String rawName) {
        TrackerEntry tracker = getTracker(rawName);
        return tracker == null ? "0" : Long.toString(tracker.killCount());
    }

    public static String uniqueCount(String rawName) {
        TrackerEntry tracker = getTracker(rawName);
        return tracker == null ? "0" : Integer.toString(tracker.itemCounts().size());
    }

    public static String dropsPerMinute(String rawName) {
        TrackerEntry tracker = getTracker(rawName);
        return tracker == null ? "0" : Long.toString(tracker.dropsPerMinute());
    }

    public static String trackersToken() {
        return String.join(", ", trackerNames());
    }

    private static String normalizeName(String rawName) {
        return rawName == null ? "" : rawName.trim().toLowerCase(Locale.ROOT);
    }

    private static String compactItemId(String itemId) {
        if (itemId == null) {
            return "";
        }
        return itemId.startsWith("minecraft:") ? itemId.substring("minecraft:".length()) : itemId;
    }
}
