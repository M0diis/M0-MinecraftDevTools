package me.m0dii.modules.debugviews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Counts block entities per type and per chunk. */
public final class BlockEntityCensus {
    public record Row(String type, int count) {
    }

    private final Map<String, Integer> byType = new HashMap<>();
    private final Map<Long, Integer> byChunk = new HashMap<>();
    private int total;

    public void add(String type, int chunkX, int chunkZ) {
        byType.merge(type, 1, Integer::sum);
        byChunk.merge(((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL), 1, Integer::sum);
        total++;
    }

    public int total() {
        return total;
    }

    /** Most common types first (ties by name), at most {@code limit}. */
    public List<Row> top(int limit) {
        List<Row> rows = new ArrayList<>();
        byType.forEach((type, count) -> rows.add(new Row(type, count)));
        rows.sort((a, b) -> a.count() != b.count() ? Integer.compare(b.count(), a.count()) : a.type().compareTo(b.type()));
        return rows.size() > limit ? new ArrayList<>(rows.subList(0, limit)) : rows;
    }

    /** "chunk 3, -2 (40) · blocks 48..63, -32..-17", or "" when empty. */
    public String busiestChunk() {
        long bestKey = 0;
        int best = 0;
        for (Map.Entry<Long, Integer> entry : byChunk.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                bestKey = entry.getKey();
            }
        }
        if (best == 0) {
            return "";
        }
        int cx = (int) (bestKey >> 32);
        int cz = (int) bestKey;
        return "chunk " + cx + ", " + cz + " (" + best + ") · blocks " + (cx << 4) + ".." + ((cx << 4) + 15)
                + ", " + (cz << 4) + ".." + ((cz << 4) + 15);
    }
}
