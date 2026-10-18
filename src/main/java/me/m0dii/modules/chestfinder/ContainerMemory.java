package me.m0dii.modules.chestfinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.*;

/**
 * What was inside each container you opened in one world: item counts by id, display names, the container
 * title and when it was seen. Pure Java (Gson for saving) so it can be tested.
 */
public final class ContainerMemory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One remembered container. {@code pos} is a packed block position (BlockPos.asLong). */
    public static final class Entry {
        public String dimension;
        public long pos;
        public String title;
        public long updatedMs;
        public Map<String, Integer> counts = new LinkedHashMap<>();
        public Map<String, String> names = new LinkedHashMap<>();

        public Entry() {
        }

        public Entry(String dimension, long pos, String title, long updatedMs) {
            this.dimension = dimension;
            this.pos = pos;
            this.title = title;
            this.updatedMs = updatedMs;
        }

        public void add(String itemId, String displayName, int count) {
            counts.merge(itemId, count, Integer::sum);
            if (displayName != null && !displayName.isBlank()) {
                names.putIfAbsent(itemId, displayName);
            }
        }

        public int total() {
            int total = 0;
            for (int count : counts.values()) {
                total += count;
            }
            return total;
        }
    }

    /** A container holding something that matched a search, with how many matching items it has. */
    public record Match(Entry entry, int count, List<String> itemIds) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    static String key(String dimension, long pos) {
        return dimension + "|" + pos;
    }

    /** Stores (or replaces) a container's contents; an empty container is still remembered as empty. */
    public synchronized void put(Entry entry) {
        entries.put(key(entry.dimension, entry.pos), entry);
    }

    public synchronized Entry get(String dimension, long pos) {
        return entries.get(key(dimension, pos));
    }

    public synchronized boolean remove(String dimension, long pos) {
        return entries.remove(key(dimension, pos)) != null;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized List<Entry> entries(String dimension) {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (dimension == null || dimension.equals(entry.dimension)) {
                out.add(entry);
            }
        }
        return out;
    }

    /**
     * Containers in {@code dimension} with items whose id or name contains the query (case-insensitive),
     * most matching items first.
     */
    public synchronized List<Match> search(String query, String dimension) {
        String q = normalize(query);
        List<Match> out = new ArrayList<>();
        if (q.isEmpty()) {
            return out;
        }
        for (Entry entry : entries.values()) {
            if (dimension != null && !dimension.equals(entry.dimension)) {
                continue;
            }
            int count = 0;
            List<String> ids = new ArrayList<>();
            for (Map.Entry<String, Integer> item : entry.counts.entrySet()) {
                if (matches(item.getKey(), entry.names.get(item.getKey()), q)) {
                    count += item.getValue();
                    ids.add(item.getKey());
                }
            }
            if (count > 0) {
                out.add(new Match(entry, count, ids));
            }
        }
        out.sort(Comparator.comparingInt(Match::count).reversed());
        return out;
    }

    /** Total of one item across containers. */
    public record Total(String itemId, String name, int count, int containers) {
    }

    /** Every remembered item in the dimension, summed over containers, most plentiful first. */
    public synchronized List<Total> totals(String dimension, String query) {
        String q = normalize(query);
        Map<String, int[]> sums = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (Entry entry : entries.values()) {
            if (dimension != null && !dimension.equals(entry.dimension)) {
                continue;
            }
            for (Map.Entry<String, Integer> item : entry.counts.entrySet()) {
                String id = item.getKey();
                if (!q.isEmpty() && !matches(id, entry.names.get(id), q)) {
                    continue;
                }
                int[] sum = sums.computeIfAbsent(id, k -> new int[2]);
                sum[0] += item.getValue();
                sum[1]++;
                if (entry.names.containsKey(id)) {
                    names.putIfAbsent(id, entry.names.get(id));
                }
            }
        }
        List<Total> out = new ArrayList<>();
        sums.forEach((id, sum) -> out.add(new Total(id, names.getOrDefault(id, id), sum[0], sum[1])));
        out.sort(Comparator.comparingInt(Total::count).reversed().thenComparing(Total::itemId));
        return out;
    }

    /** Item ids (and names) seen in this dimension, for command suggestions. */
    public synchronized List<String> knownItems(String dimension) {
        List<String> out = new ArrayList<>();
        for (Entry entry : entries.values()) {
            if (dimension == null || dimension.equals(entry.dimension)) {
                for (String id : entry.counts.keySet()) {
                    String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
                    if (!out.contains(path)) {
                        out.add(path);
                    }
                }
            }
        }
        out.sort(null);
        return out;
    }

    static boolean matches(String itemId, String name, String query) {
        String id = itemId.toLowerCase(Locale.ROOT);
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (path.contains(query) || id.equals(query) || path.replace('_', ' ').contains(query.replace('_', ' '))) {
            return true;
        }
        return name != null && normalize(name).contains(query.replace('_', ' '));
    }

    private static String normalize(String text) {
        return text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }

    public synchronized String toJson() {
        return GSON.toJson(new ArrayList<>(entries.values()));
    }

    public static ContainerMemory fromJson(String json) {
        ContainerMemory memory = new ContainerMemory();
        if (json == null || json.isBlank()) {
            return memory;
        }
        List<Entry> list = GSON.fromJson(json, new TypeToken<List<Entry>>() { }.getType());
        if (list != null) {
            for (Entry entry : list) {
                if (entry != null && entry.dimension != null) {
                    if (entry.counts == null) {
                        entry.counts = new LinkedHashMap<>();
                    }
                    if (entry.names == null) {
                        entry.names = new LinkedHashMap<>();
                    }
                    memory.put(entry);
                }
            }
        }
        return memory;
    }
}
