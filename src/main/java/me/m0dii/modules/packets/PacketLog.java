package me.m0dii.modules.packets;

import java.util.*;

/**
 * Thread-safe ring buffer of recent packets plus per-type counters. Packets arrive on the network thread;
 * the screen reads snapshots on the client thread. Holds plain strings only, never packet objects.
 */
public final class PacketLog {
    public enum Direction {
        IN("◀ in"),
        OUT("▶ out");

        public final String label;

        Direction(String label) {
            this.label = label;
        }
    }

    public record Entry(long id, long timeMs, Direction direction, String name, String details) {
    }

    /** Packets that arrive many times a second and drown everything else. Hidden while "hide noisy" is on. */
    static final Set<String> NOISY = Set.of(
            "ClientboundMoveEntityPacket", "ClientboundMoveEntityPacket.Pos", "ClientboundMoveEntityPacket.Rot",
            "ClientboundMoveEntityPacket.PosRot", "ClientboundRotateHeadPacket", "ClientboundSetEntityMotionPacket",
            "ClientboundTeleportEntityPacket", "ClientboundEntityPositionSyncPacket", "ClientboundSetTimePacket",
            "ClientboundKeepAlivePacket", "ServerboundKeepAlivePacket", "ClientboundPingPacket", "ServerboundPongPacket",
            "ServerboundMovePlayerPacket", "ServerboundMovePlayerPacket.Pos", "ServerboundMovePlayerPacket.Rot",
            "ServerboundMovePlayerPacket.PosRot", "ServerboundMovePlayerPacket.StatusOnly", "ServerboundClientTickEndPacket",
            "ClientboundBundlePacket", "ClientboundBundleDelimiterPacket", "ClientboundSetEntityDataPacket",
            "ClientboundUpdateAttributesPacket", "ClientboundLevelChunkWithLightPacket", "ClientboundLightUpdatePacket",
            "ClientboundChunkBatchStartPacket", "ClientboundChunkBatchFinishedPacket", "ServerboundChunkBatchReceivedPacket",
            "ServerboundPlayerInputPacket", "ClientboundSoundPacket", "ClientboundLevelParticlesPacket");

    private final int capacity;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private final Map<String, Long> counts = new LinkedHashMap<>();
    /** Bytes per entry id, when known (sizes of outgoing packets arrive after the entry is made). */
    private final Map<Long, Integer> sizes = new HashMap<>();
    private final Map<String, Long> bytesByType = new HashMap<>();
    private final PacketTraffic bytesIn = new PacketTraffic();
    private final PacketTraffic bytesOut = new PacketTraffic();
    private final PacketTraffic packetsIn = new PacketTraffic();
    private final PacketTraffic packetsOut = new PacketTraffic();
    private final PacketTraffic pinnedRate = new PacketTraffic();
    private volatile String pinned;
    private long nextId = 1;
    private volatile boolean recording;

    public PacketLog(int capacity) {
        this.capacity = Math.max(10, capacity);
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public synchronized void add(Direction direction, String name, String details, long timeMs) {
        add(direction, name, details, timeMs, -1);
    }

    /** Records a packet; {@code bytes} is its size or -1 when not known (yet). Returns the entry id, or -1. */
    public synchronized long add(Direction direction, String name, String details, long timeMs, int bytes) {
        if (!recording) {
            return -1;
        }
        long id = nextId++;
        entries.addLast(new Entry(id, timeMs, direction, name, details));
        while (entries.size() > capacity) {
            sizes.remove(entries.removeFirst().id());
        }
        counts.merge(direction.name() + " " + name, 1L, Long::sum);
        (direction == Direction.IN ? packetsIn : packetsOut).add(1, timeMs);
        if (name.equals(pinned)) {
            pinnedRate.add(1, timeMs);
        }
        if (bytes >= 0) {
            recordSize(id, direction, name, bytes, timeMs);
        }
        return id;
    }

    /** Adds the size of an already recorded packet (outgoing packets are measured after they are logged). */
    public synchronized void setSize(long id, Direction direction, String name, int bytes, long timeMs) {
        if (id < 0 || sizes.containsKey(id) || !recording) {
            return;
        }
        boolean present = !entries.isEmpty() && id >= entries.peekFirst().id();
        if (present) {
            recordSize(id, direction, name, bytes, timeMs);
        }
    }

    private void recordSize(long id, Direction direction, String name, int bytes, long timeMs) {
        sizes.put(id, bytes);
        bytesByType.merge(direction.name() + " " + name, (long) bytes, Long::sum);
    }

    /** Bytes on the connection, counted where packets are encoded/decoded (independent of the entry list). */
    public void countBytes(Direction direction, int bytes, long timeMs) {
        if (recording && bytes > 0) {
            (direction == Direction.IN ? bytesIn : bytesOut).add(bytes, timeMs);
        }
    }

    /** Size in bytes of the entry, or -1 when unknown (e.g. singleplayer, where packets are not encoded). */
    public synchronized int sizeOf(long id) {
        return sizes.getOrDefault(id, -1);
    }

    public synchronized void clear() {
        entries.clear();
        counts.clear();
        sizes.clear();
        bytesByType.clear();
        bytesIn.clear();
        bytesOut.clear();
        packetsIn.clear();
        packetsOut.clear();
        pinnedRate.clear();
    }

    // ---- traffic ------------------------------------------------------------------------------------------

    public PacketTraffic bytesIn() {
        return bytesIn;
    }

    public PacketTraffic bytesOut() {
        return bytesOut;
    }

    public PacketTraffic packetsIn() {
        return packetsIn;
    }

    public PacketTraffic packetsOut() {
        return packetsOut;
    }

    /** Graphs one packet type per second (null stops). */
    public synchronized void pin(String name) {
        pinned = name == null || name.isBlank() ? null : name.trim();
        pinnedRate.clear();
    }

    public String pinned() {
        return pinned;
    }

    public PacketTraffic pinnedRate() {
        return pinnedRate;
    }

    /** Packet type names seen since the last clear (for command suggestions). */
    public synchronized List<String> knownNames() {
        TreeSet<String> names = new TreeSet<>();
        for (String key : counts.keySet()) {
            names.add(key.substring(key.indexOf(' ') + 1));
        }
        return new ArrayList<>(names);
    }

    /** Types that used the most bytes since the last clear, highest first. */
    public synchronized List<Map.Entry<String, Long>> topBytes(int limit) {
        return bytesByType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
    }

    /** Tab-separated lines, oldest first: time, direction, packet, bytes, details. */
    public synchronized String export(java.text.SimpleDateFormat time) {
        StringBuilder out = new StringBuilder("time\tdirection\tpacket\tbytes\tdetails\n");
        for (Entry entry : entries) {
            int size = sizes.getOrDefault(entry.id(), -1);
            out.append(time.format(new Date(entry.timeMs()))).append('\t')
                    .append(entry.direction().name()).append('\t')
                    .append(entry.name()).append('\t')
                    .append(size < 0 ? "" : Integer.toString(size)).append('\t')
                    .append(entry.details().replace('\t', ' ')).append('\n');
        }
        return out.toString();
    }

    public synchronized int size() {
        return entries.size();
    }

    /**
     * Newest first. {@code filter} is space-separated words that must all appear in the name or details;
     * a word starting with '-' excludes matches.
     */
    public synchronized List<Entry> snapshot(String filter, boolean showIn, boolean showOut, boolean hideNoisy) {
        String[] words = filter == null || filter.isBlank() ? new String[0] : filter.trim().toLowerCase(Locale.ROOT).split("\\s+");
        List<Entry> result = new ArrayList<>();
        var iterator = entries.descendingIterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if ((entry.direction() == Direction.IN && !showIn) || (entry.direction() == Direction.OUT && !showOut)) {
                continue;
            }
            if (hideNoisy && NOISY.contains(entry.name())) {
                continue;
            }
            if (matches(entry, words)) {
                result.add(entry);
            }
        }
        return result;
    }

    /** Most frequent packet types since the last clear, highest first. */
    public synchronized List<Map.Entry<String, Long>> topCounts(int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
    }

    static boolean matches(Entry entry, String[] words) {
        String haystack = (entry.name() + " " + entry.details()).toLowerCase(Locale.ROOT);
        for (String word : words) {
            if (word.startsWith("-") && word.length() > 1) {
                if (haystack.contains(word.substring(1))) {
                    return false;
                }
            } else if (!haystack.contains(word)) {
                return false;
            }
        }
        return true;
    }

    /** Readable packet name: nested classes keep their outer name, e.g. "ServerboundMovePlayerPacket.Pos". */
    public static String nameOf(Class<?> type) {
        String name = type.getName();
        int dot = name.lastIndexOf('.');
        return (dot >= 0 ? name.substring(dot + 1) : name).replace('$', '.');
    }

    /** First {@code max} characters of a packet's toString(), on one line. */
    public static String describe(Object packet, int max) {
        String text;
        try {
            text = String.valueOf(packet);
        } catch (RuntimeException e) {
            text = "<toString failed: " + e.getClass().getSimpleName() + ">";
        }
        text = text.replace('\n', ' ').replace('\r', ' ');
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }
}
