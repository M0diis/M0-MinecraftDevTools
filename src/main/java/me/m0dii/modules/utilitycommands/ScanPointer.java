package me.m0dii.modules.utilitycommands;

/**
 * Direction helpers for the scan/find pointers: which way to turn to face a target.
 */
public final class ScanPointer {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private ScanPointer() {
    }

    /** Degrees to turn (negative = left, positive = right, -180..180) from a Minecraft yaw to face (dx, dz). */
    public static double relativeYaw(double playerYaw, double dx, double dz) {
        double target = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = (target - playerYaw) % 360.0;
        if (relative < -180.0) {
            relative += 360.0;
        } else if (relative >= 180.0) {
            relative -= 360.0;
        }
        return relative;
    }

    /** An arrow glyph pointing at the target relative to where the player faces. */
    public static String arrow(double playerYaw, double dx, double dz) {
        if (Math.abs(dx) < 1.0E-6 && Math.abs(dz) < 1.0E-6) {
            return "•";
        }
        int index = (int) Math.floorMod(Math.round(relativeYaw(playerYaw, dx, dz) / 45.0), 8L);
        return ARROWS[index];
    }

    /** "12 m" / "1.2 km" with an "above"/"below" hint for large height differences. */
    public static String distance(double dx, double dy, double dz) {
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        String text = d >= 1000 ? String.format(java.util.Locale.ROOT, "%.1f km", d / 1000.0) : Math.round(d) + " m";
        if (dy >= 4) {
            text += " ▲" + Math.round(dy);
        } else if (dy <= -4) {
            text += " ▼" + Math.round(-dy);
        }
        return text;
    }
}
