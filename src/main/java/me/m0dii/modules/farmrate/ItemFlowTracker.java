package me.m0dii.modules.farmrate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Turns per-tick snapshots of the item entities inside a zone into "items that arrived". A new entity counts
 * with its stack size. An entity that grows counts the growth, minus what vanished from the zone in the same
 * tick (that is two stacks merging, not new items).
 */
public final class ItemFlowTracker {
    public record Seen(String item, int count) {
    }

    private Map<Integer, Seen> previous = new HashMap<>();

    public void reset() {
        previous = new HashMap<>();
    }

    /** {@code current}: entity id → item and stack size now. Returns arrived items per item id. */
    public Map<String, Long> update(Map<Integer, Seen> current) {
        Map<String, Long> arrived = new HashMap<>();
        Map<String, Long> growth = new HashMap<>();
        Map<String, Long> vanished = new HashMap<>();
        for (Map.Entry<Integer, Seen> entry : current.entrySet()) {
            Seen now = entry.getValue();
            Seen before = previous.get(entry.getKey());
            if (before == null) {
                arrived.merge(now.item(), (long) now.count(), Long::sum);
            } else if (now.count() > before.count() && now.item().equals(before.item())) {
                growth.merge(now.item(), (long) (now.count() - before.count()), Long::sum);
            }
        }
        Set<Integer> stillHere = current.keySet();
        for (Map.Entry<Integer, Seen> entry : previous.entrySet()) {
            if (!stillHere.contains(entry.getKey())) {
                vanished.merge(entry.getValue().item(), (long) entry.getValue().count(), Long::sum);
            }
        }
        growth.forEach((item, amount) -> {
            long extra = amount - vanished.getOrDefault(item, 0L);
            if (extra > 0) {
                arrived.merge(item, extra, Long::sum);
            }
        });
        previous = new HashMap<>(current);
        return arrived;
    }
}
