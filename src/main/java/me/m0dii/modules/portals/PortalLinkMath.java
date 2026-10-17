package me.m0dii.modules.portals;

import java.util.List;

/**
 * Approximates vanilla nether portal linking: the destination point is the portal position scaled by 1/8
 * (overworld → nether) or 8 (nether → overworld); the game then uses the closest existing portal within
 * 16 blocks (in the nether) or 128 blocks (in the overworld) of that point, measured in a square, or builds a
 * new one nearby.
 */
public final class PortalLinkMath {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";

    private PortalLinkMath() {
    }

    /** The dimension a portal in {@code dimension} leads to, or {@code null}. */
    public static String destination(String dimension) {
        if (OVERWORLD.equals(dimension)) {
            return NETHER;
        }
        if (NETHER.equals(dimension)) {
            return OVERWORLD;
        }
        return null;
    }

    public static double scale(String fromDimension) {
        return NETHER.equals(fromDimension) ? 8.0 : 1.0 / 8.0;
    }

    public static int searchRadius(String destinationDimension) {
        return NETHER.equals(destinationDimension) ? 16 : 128;
    }

    public record Target(double x, double y, double z, String dimension) {
    }

    /** Where the game looks for a portal when entering {@code portal}. */
    public static Target targetOf(KnownPortal portal) {
        String destination = destination(portal.dimension);
        if (destination == null) {
            return null;
        }
        double scale = scale(portal.dimension);
        return new Target(portal.centerX() * scale, portal.centerY(), portal.centerZ() * scale, destination);
    }

    /** Closest known portal the game would pick for {@code target}, or {@code null} if none is known in range. */
    public static KnownPortal pick(List<KnownPortal> candidates, String world, Target target) {
        if (target == null) {
            return null;
        }
        int radius = searchRadius(target.dimension());
        KnownPortal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (KnownPortal candidate : candidates) {
            if (!candidate.sameWorldAndDimension(world, target.dimension())) {
                continue;
            }
            boolean inSquare = candidate.maxX >= target.x() - radius && candidate.minX <= target.x() + radius
                    && candidate.maxZ >= target.z() - radius && candidate.minZ <= target.z() + radius;
            if (!inSquare) {
                continue;
            }
            double distance = candidate.distanceSq(target.x(), target.y(), target.z());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }
}
