package me.m0dii.modules.bridging;

public enum BridgingAxisModeOverride {
    HORIZONTAL(BridgingAxisMode.HORIZONTAL),
    VERTICAL(BridgingAxisMode.VERTICAL),
    BOTH(BridgingAxisMode.BOTH),
    FALLBACK(null);

    private final BridgingAxisMode mode;

    BridgingAxisModeOverride(BridgingAxisMode mode) {
        this.mode = mode;
    }

    public BridgingAxisMode resolve(BridgingAxisMode fallback) {
        return this.mode == null ? fallback : this.mode;
    }

    public BridgingAxisModeOverride next() {
        BridgingAxisModeOverride[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
