package me.m0dii.modules.bridging;

public enum BridgingPerspectiveLock {
    LET_BRIDGING_DECIDE,
    COPY_TOGGLE_PERSPECTIVE,
    ALWAYS_EYELINE;

    public BridgingPerspectiveLock next() {
        BridgingPerspectiveLock[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
