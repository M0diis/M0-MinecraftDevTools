package me.m0dii.modules.redstonetimeline;

import java.util.*;

/**
 * Signal history of a set of blocks, one track per block. Only changes are stored, so a track is a list of
 * (tick, value) steps. Values are 0-15 (on/off blocks use 0 and 15).
 */
public final class RedstoneTimeline {
    public static final int MAX_CHANGES_PER_TRACK = 20_000;

    public record Step(long tick, int value) {
    }

    /** A stretch of ticks with one value; {@code endTick} is exclusive ({@code Long.MAX_VALUE} while ongoing). */
    public record Segment(long startTick, long endTick, int value) {
        public long length(long nowTick) {
            return Math.max(0, Math.min(endTick, nowTick + 1) - startTick);
        }
    }

    public static final class Track {
        private final long key;
        private final String label;
        private final List<Step> steps = new ArrayList<>();
        private int dropped;

        Track(long key, String label) {
            this.key = key;
            this.label = label;
        }

        public long key() {
            return key;
        }

        public String label() {
            return label;
        }

        public long firstStepTick() {
            return steps.isEmpty() ? Long.MAX_VALUE : steps.get(0).tick();
        }

        public List<Step> steps() {
            return Collections.unmodifiableList(steps);
        }

        public int changes() {
            return Math.max(0, steps.size() - 1) + dropped;
        }

        public int lastValue() {
            return steps.isEmpty() ? 0 : steps.get(steps.size() - 1).value();
        }

        /** Value at {@code tick}, or -1 before the track's first sample. */
        public int valueAt(long tick) {
            int index = indexAt(tick);
            return index < 0 ? -1 : steps.get(index).value();
        }

        public Segment segmentAt(long tick) {
            int index = indexAt(tick);
            if (index < 0) {
                return null;
            }
            long end = index + 1 < steps.size() ? steps.get(index + 1).tick() : Long.MAX_VALUE;
            return new Segment(steps.get(index).tick(), end, steps.get(index).value());
        }

        /** Index of the last step at or before {@code tick}. */
        private int indexAt(long tick) {
            int lo = 0;
            int hi = steps.size() - 1;
            int found = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (steps.get(mid).tick() <= tick) {
                    found = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return found;
        }

        private boolean record(long tick, int value) {
            if (!steps.isEmpty()) {
                Step last = steps.get(steps.size() - 1);
                if (last.value() == value) {
                    return false;
                }
                if (last.tick() == tick) {
                    // Several changes inside one tick: keep the final value.
                    steps.set(steps.size() - 1, new Step(tick, value));
                    if (steps.size() >= 2 && steps.get(steps.size() - 2).value() == value) {
                        steps.remove(steps.size() - 1);
                    }
                    return true;
                }
            }
            if (steps.size() >= MAX_CHANGES_PER_TRACK) {
                steps.remove(0);
                dropped++;
            }
            steps.add(new Step(tick, value));
            return true;
        }
    }

    private final int maxTracks;
    private final Map<Long, Track> tracks = new LinkedHashMap<>();
    private long firstTick = Long.MAX_VALUE;
    private long lastTick = Long.MIN_VALUE;

    public RedstoneTimeline(int maxTracks) {
        this.maxTracks = maxTracks;
    }

    public synchronized void clear() {
        tracks.clear();
        firstTick = Long.MAX_VALUE;
        lastTick = Long.MIN_VALUE;
    }

    public synchronized boolean isTracked(long key) {
        return tracks.containsKey(key);
    }

    public synchronized boolean isFull() {
        return tracks.size() >= maxTracks;
    }

    /**
     * Stores {@code value} for {@code key} at {@code tick} if it changed. New keys are added while there is room.
     * Returns true when something was stored.
     */
    public synchronized boolean record(long key, String label, long tick, int value) {
        Track track = tracks.get(key);
        if (track == null) {
            if (tracks.size() >= maxTracks) {
                return false;
            }
            track = new Track(key, label);
            tracks.put(key, track);
        }
        noteTick(tick);
        return track.record(tick, value);
    }

    /** Marks that {@code tick} was observed, even if nothing changed. */
    public synchronized void noteTick(long tick) {
        firstTick = Math.min(firstTick, tick);
        lastTick = Math.max(lastTick, tick);
    }

    public synchronized void remove(long key) {
        tracks.remove(key);
    }

    public synchronized List<Track> tracks() {
        return new ArrayList<>(tracks.values());
    }

    /** Tracks that changed at least once, in the order they were added (nearest to the start point first). */
    public synchronized List<Track> activeTracks() {
        List<Track> active = new ArrayList<>();
        for (Track track : tracks.values()) {
            if (track.changes() > 0) {
                active.add(track);
            }
        }
        return active;
    }

    public synchronized boolean isEmpty() {
        return firstTick == Long.MAX_VALUE;
    }

    public synchronized long firstTick() {
        return firstTick;
    }

    public synchronized long lastTick() {
        return lastTick;
    }

    /** "4 gt (2 rt)". */
    public static String describeTicks(long gameTicks) {
        if (gameTicks % 2 == 0) {
            return gameTicks + " gt (" + gameTicks / 2 + " rt)";
        }
        return gameTicks + " gt (" + gameTicks / 2 + ".5 rt)";
    }
}
