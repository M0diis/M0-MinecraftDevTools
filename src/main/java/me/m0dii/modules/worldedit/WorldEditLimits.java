package me.m0dii.modules.worldedit;

import java.util.List;

final class WorldEditLimits {
    static final long MAX_VOLUME = 262_144;
    static final long MAX_SNAPSHOT_BYTES = 16L * 1024 * 1024;

    private WorldEditLimits() { }

    static void validate(List<WorldEditSelection> regions, int minY, int maxY) {
        long total = 0;
        for (WorldEditSelection region : regions) {
            if (region.min().getY() < minY || region.max().getY() >= maxY) {
                throw new IllegalArgumentException("Selection must stay within build height " + minY + ".." + (maxY - 1) + ".");
            }
            long volume = volume(region);
            if (volume > MAX_VOLUME - total) {
                throw new IllegalArgumentException("Edit exceeds the " + MAX_VOLUME + " block work limit.");
            }
            total += volume;
        }
    }

    static long volume(WorldEditSelection region) {
        try {
            long x = (long) region.max().getX() - region.min().getX() + 1;
            long y = (long) region.max().getY() - region.min().getY() + 1;
            long z = (long) region.max().getZ() - region.min().getZ() + 1;
            return Math.multiplyExact(Math.multiplyExact(x, y), z);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Selection is too large.");
        }
    }

    static void validateCopies(WorldEditSelection source, int copies) {
        long volume = volume(source);
        if (copies < 1 || volume > MAX_VOLUME / ((long) copies + 1)) {
            throw new IllegalArgumentException("Stack exceeds the " + MAX_VOLUME + " block work limit.");
        }
    }
}
