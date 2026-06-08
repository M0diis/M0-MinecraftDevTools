package me.m0dii.modules.watson;

import lombok.Getter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Stores parsed CoreProtect hits and keeps the list bounded.
 */
public final class CoreProtectTracker {
    private static final int MAX_ENTRIES = 256;
    private static final ConcurrentLinkedDeque<CoreProtectEntry> ENTRIES = new ConcurrentLinkedDeque<>();
    private static final CoreProtectParser PARSER = new CoreProtectParser();

    @Getter
    private static volatile long ttlMs = 5 * 60_000L;

    private CoreProtectTracker() {
        // Utility class
    }

    public static void onChatMessage(Text message) {
        if (message == null) {
            return;
        }

        ingestRawLine(message.getString(), true);
    }

    public static int injectSyntheticLookup(int x, int y, int z, @NotNull String world) {
        String safeWorld = world.trim().isEmpty() ? "world" : world.trim();

        int added = 0;
        if (ingestRawLine("----- CoreProtect Lookup Results -----", false).isPresent()) {
            added++;
        }
        if (ingestRawLine("0.01/h ago - TestUser removed #4 (Cobblestone).", false).isPresent()) {
            added++;
        }
        if (ingestRawLine(String.format("    ^ (x%d/y%d/z%d/%s)", x, y, z, safeWorld), false).isPresent()) {
            added++;
        }

        return added;
    }

    public static Optional<CoreProtectEntry> ingestRawLine(String rawLine, boolean requireModuleEnabled) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }
        if (requireModuleEnabled && !WatsonCoreProtectModule.INSTANCE.isEnabled()) {
            return Optional.empty();
        }

        synchronized (PARSER) {
            Optional<CoreProtectEntry> parsed = PARSER.parse(rawLine);
            parsed.ifPresent(entry -> {
                ENTRIES.addFirst(entry);
                trim();
            });
            return parsed;
        }
    }

    public static List<CoreProtectEntry> snapshot() {
        trim();
        return new ArrayList<>(ENTRIES);
    }

    public static int size() {
        trim();
        return ENTRIES.size();
    }

    public static List<BlockPos> getOrderedPositions() {
        trim();
        LinkedHashSet<BlockPos> unique = new LinkedHashSet<>();
        for (CoreProtectEntry entry : ENTRIES) {
            unique.add(entry.pos());
        }

        List<BlockPos> ordered = new ArrayList<>(unique);
        ordered.sort(Comparator
                .comparingInt(BlockPos::getX)
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ));
        return ordered;
    }

    public static Map<BlockPos, Integer> getPositionIndexMap() {
        List<BlockPos> ordered = getOrderedPositions();
        Map<BlockPos, Integer> ids = new HashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            ids.put(ordered.get(i), i + 1);
        }
        return ids;
    }

    public static Optional<BlockPos> getPositionById(int id) {
        if (id < 1) {
            return Optional.empty();
        }
        List<BlockPos> ordered = getOrderedPositions();
        if (id > ordered.size()) {
            return Optional.empty();
        }
        return Optional.of(ordered.get(id - 1));
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static void setTtlSeconds(int seconds) {
        ttlMs = Math.max(5, seconds) * 1000L;
        trim();
    }

    private static void trim() {
        long cutoff = System.currentTimeMillis() - ttlMs;
        while (true) {
            CoreProtectEntry tail = ENTRIES.peekLast();
            if (tail == null) {
                break;
            }
            if (tail.observedAt() >= cutoff && ENTRIES.size() <= MAX_ENTRIES) {
                break;
            }
            ENTRIES.pollLast();
        }
    }
}

