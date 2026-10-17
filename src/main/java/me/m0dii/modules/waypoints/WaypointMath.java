package me.m0dii.modules.waypoints;

/** Coordinate and direction helpers for waypoints; plain math so it can be unit tested. */
public final class WaypointMath {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private WaypointMath() {
    }

    /**
     * Scale factor to show a waypoint saved in {@code fromDimension} while you are in {@code toDimension}:
     * 1 for the same dimension, 1/8 overworld → nether, 8 nether → overworld, 0 when there is no mapping.
     */
    public static double scale(String fromDimension, String toDimension) {
        if (fromDimension == null || toDimension == null) {
            return 0.0;
        }
        if (fromDimension.equals(toDimension)) {
            return 1.0;
        }
        if (OVERWORLD.equals(fromDimension) && NETHER.equals(toDimension)) {
            return 1.0 / 8.0;
        }
        if (NETHER.equals(fromDimension) && OVERWORLD.equals(toDimension)) {
            return 8.0;
        }
        return 0.0;
    }

    /**
     * Minecraft yaw that faces from (0,0) towards (dx,dz): 0 = south (+Z), 90 = west (-X), -90 = east (+X).
     */
    public static double yawTowards(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Wraps an angle to [-180, 180). */
    public static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    /**
     * Arrow pointing at the target relative to where the player looks: ↑ ahead, → to the right, ↓ behind.
     */
    public static String arrowFor(double dx, double dz, double playerYaw) {
        double relative = wrapDegrees(yawTowards(dx, dz) - playerYaw);
        int index = (int) Math.floorMod(Math.round(relative / 45.0), 8L);
        return ARROWS[index];
    }

    /** A stable colour per waypoint id (bright, opaque RGB). */
    public static int colorFor(String id) {
        int hash = id == null ? 0 : id.hashCode();
        float hue = (hash & 0xFFFF) / 65535.0f;
        return hsvToRgb(hue, 0.65f, 1.0f);
    }

    static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) h;
        float f = h - sector;
        float p = value * (1 - saturation);
        float q = value * (1 - saturation * f);
        float t = value * (1 - saturation * (1 - f));
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
