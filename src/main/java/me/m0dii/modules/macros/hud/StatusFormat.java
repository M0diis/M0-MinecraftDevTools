package me.m0dii.modules.macros.hud;

import java.util.Locale;

/**
 * Pure formatting helpers for the status HUD widget (durability, effect timers), kept free of Minecraft
 * types so they can be unit tested.
 */
public final class StatusFormat {
    /** What a status widget shows. */
    public enum Kind {
        ARMOR("Armor"), HELD("Held items"), EFFECTS("Effects"), ALERT("Durability alert");

        public final String label;

        Kind(String label) {
            this.label = label;
        }

        public static Kind parse(String raw) {
            if (raw != null) {
                for (Kind kind : values()) {
                    if (kind.name().equalsIgnoreCase(raw.trim())) {
                        return kind;
                    }
                }
            }
            return ARMOR;
        }

        public Kind next(boolean forward) {
            Kind[] all = values();
            return all[Math.floorMod(ordinal() + (forward ? 1 : -1), all.length)];
        }
    }

    /** How durability is written next to an item. */
    public enum TextMode {
        PERCENT("Percent"), REMAINING("Remaining"), BOTH("Both"), NONE("Bar only");

        public final String label;

        TextMode(String label) {
            this.label = label;
        }

        public static TextMode parse(String raw) {
            if (raw != null) {
                for (TextMode mode : values()) {
                    if (mode.name().equalsIgnoreCase(raw.trim())) {
                        return mode;
                    }
                }
            }
            return PERCENT;
        }

        public TextMode next(boolean forward) {
            TextMode[] all = values();
            return all[Math.floorMod(ordinal() + (forward ? 1 : -1), all.length)];
        }
    }

    private StatusFormat() {
    }

    /** Remaining durability as a fraction 0..1 (1 for items without durability). */
    public static double fraction(int maxDamage, int damage) {
        if (maxDamage <= 0) {
            return 1.0;
        }
        return Math.clamp((maxDamage - damage) / (double) maxDamage, 0.0, 1.0);
    }

    public static int percent(int maxDamage, int damage) {
        return (int) Math.round(fraction(maxDamage, damage) * 100.0);
    }

    /** Durability text for the given mode; empty when the item has no durability or the mode is NONE. */
    public static String durability(TextMode mode, int maxDamage, int damage) {
        if (maxDamage <= 0 || mode == TextMode.NONE) {
            return "";
        }
        int remaining = Math.max(0, maxDamage - damage);
        return switch (mode) {
            case PERCENT -> percent(maxDamage, damage) + "%";
            case REMAINING -> Integer.toString(remaining);
            case BOTH -> remaining + " (" + percent(maxDamage, damage) + "%)";
            case NONE -> "";
        };
    }

    /** Green → yellow → red gradient for a remaining fraction, as an opaque ARGB colour. */
    public static int durabilityColor(double fraction) {
        double f = Math.clamp(fraction, 0.0, 1.0);
        int r;
        int g;
        if (f >= 0.5) {
            r = (int) Math.round(255 * (1.0 - f) * 2.0);
            g = 255;
        } else {
            r = 255;
            g = (int) Math.round(255 * f * 2.0);
        }
        return 0xFF000000 | (Math.clamp(r, 0, 255) << 16) | (Math.clamp(g, 0, 255) << 8) | 0x40;
    }

    /** True when an item with durability is at or below the warning percentage. */
    public static boolean isLow(int maxDamage, int damage, int warnPercent) {
        return maxDamage > 0 && fraction(maxDamage, damage) * 100.0 <= warnPercent;
    }

    /** Effect time in ticks as "m:ss" (or "h:mm:ss"); "∞" for infinite effects (negative duration). */
    public static String duration(int ticks) {
        if (ticks < 0) {
            return "∞";
        }
        int total = ticks / 20;
        int h = total / 3600;
        int m = (total / 60) % 60;
        int s = total % 60;
        return h > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
                : String.format(Locale.ROOT, "%d:%02d", m, s);
    }

    /** Effect level from its amplifier (0 → "", 1 → "II", ... beyond 10 plain digits). */
    public static String level(int amplifier) {
        int level = amplifier + 1;
        if (level <= 1) {
            return "";
        }
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return level < roman.length ? roman[level] : Integer.toString(level);
    }

    /** Effects about to run out blink; this decides the on/off phase for a given clock. */
    public static boolean blinkVisible(int ticks, long timeMs) {
        if (ticks < 0 || ticks > 200) {
            return true;
        }
        long period = ticks < 60 ? 250 : 500;
        return (timeMs / period) % 2 == 0;
    }
}
