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
        if (!recording) {
            return;
        }
        entries.addLast(new Entry(nextId++, timeMs, direction, name, details));
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
        counts.merge(direction.name() + " " + name, 1L, Long::sum);
    }

    public synchronized void clear() {
        entries.clear();
        counts.clear();
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
