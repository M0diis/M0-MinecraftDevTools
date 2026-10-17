package me.m0dii.modules.placement;

import java.util.Locale;

/**
 * Keeps a stream of block placements (use key held down) on one shape, decided by the first block placed:
 * <ul>
 *     <li>PLANE: the layer of the first block, across the face you clicked (a floor, a wall).</li>
 *     <li>FACE: like PLANE, but only when clicking the same face direction as the first time.</li>
 *     <li>COLUMN: straight out from the clicked face (pillars, towers).</li>
 *     <li>LINE: a straight row inside the plane; the direction is set by the second block.</li>
 * </ul>
 */
public final class PlacementRestriction {
    public enum Mode {
        OFF, PLANE, FACE, COLUMN, LINE;

        public String label() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public static Mode parse(String text) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(text == null ? "" : text.trim())) {
                    return mode;
                }
            }
            return null;
        }
    }

    public record Pos(int x, int y, int z) {
        int get(char axis) {
            return switch (axis) {
                case 'x' -> x;
                case 'y' -> y;
                default -> z;
            };
        }
    }

    private Pos anchor;
    private String anchorFace;
    private char faceAxis;
    private char lineAxis;

    /** Forget the first block (the use key was released). */
    public void reset() {
        anchor = null;
        anchorFace = null;
        lineAxis = 0;
    }

    public boolean hasAnchor() {
        return anchor != null;
    }

    public Pos anchor() {
        return anchor;
    }

    public char faceAxis() {
        return faceAxis;
    }

    public char lineAxis() {
        return lineAxis;
    }

    /**
     * Whether a block may go at {@code place}, clicked on face {@code face} ("up", "north", ...) whose axis is
     * {@code axis} ('x', 'y' or 'z'). The first call of a stream always passes and becomes the anchor.
     */
    public boolean allow(Mode mode, Pos place, String face, char axis) {
        if (mode == null || mode == Mode.OFF) {
            return true;
        }
        if (anchor == null) {
            anchor = place;
            anchorFace = face;
            faceAxis = axis;
            lineAxis = 0;
            return true;
        }
        boolean samePlane = place.get(faceAxis) == anchor.get(faceAxis);
        return switch (mode) {
            case PLANE -> samePlane;
            case FACE -> samePlane && anchorFace.equals(face);
            case COLUMN -> differingAxes(place).replace(String.valueOf(faceAxis), "").isEmpty();
            case LINE -> allowLine(place, samePlane);
            default -> true;
        };
    }

    private boolean allowLine(Pos place, boolean samePlane) {
        if (!samePlane) {
            return false;
        }
        String diff = differingAxes(place);
        if (diff.isEmpty()) {
            return true;
        }
        if (diff.length() != 1) {
            return false;
        }
        if (lineAxis == 0) {
            lineAxis = diff.charAt(0);
            return true;
        }
        return diff.charAt(0) == lineAxis;
    }

    private String differingAxes(Pos place) {
        StringBuilder out = new StringBuilder();
        if (place.x() != anchor.x()) {
            out.append('x');
        }
        if (place.y() != anchor.y()) {
            out.append('y');
        }
        if (place.z() != anchor.z()) {
            out.append('z');
        }
        return out.toString();
    }

    /** Short description of the active limit, e.g. "plane y=64". */
    public String describe(Mode mode) {
        if (anchor == null || mode == null || mode == Mode.OFF) {
            return mode == null ? "" : mode.label();
        }
        return switch (mode) {
            case PLANE, FACE -> mode.label() + " " + faceAxis + "=" + anchor.get(faceAxis);
            case COLUMN -> "column along " + faceAxis;
            case LINE -> lineAxis == 0 ? "line (place a 2nd block)" : "line along " + lineAxis;
            default -> mode.label();
        };
    }
}
