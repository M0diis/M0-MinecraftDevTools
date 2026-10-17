package me.m0dii.modules.ruler;

import java.util.Locale;

/** Measurements between two block positions (both blocks included). */
public record RulerMath(int sizeX, int sizeY, int sizeZ, double distance, int manhattan) {

    public static RulerMath between(int ax, int ay, int az, int bx, int by, int bz) {
        int dx = Math.abs(bx - ax);
        int dy = Math.abs(by - ay);
        int dz = Math.abs(bz - az);
        return new RulerMath(dx + 1, dy + 1, dz + 1, Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz), dx + dy + dz);
    }

    public long volume() {
        return (long) sizeX * sizeY * sizeZ;
    }

    /** Footprint seen from above. */
    public long area() {
        return (long) sizeX * sizeZ;
    }

    public String summary() {
        return String.format(Locale.ROOT, "%d × %d × %d  ·  %,d blocks  ·  area %,d  ·  %.1fm  ·  walk %d",
                sizeX, sizeY, sizeZ, volume(), area(), distance, manhattan);
    }
}
