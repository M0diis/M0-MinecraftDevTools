package me.m0dii.modules.mousetweaks;

public enum MouseTweaksWheelScrollDirection {
    NORMAL,
    INVERTED,
    INVENTORY_POSITION_AWARE,
    INVENTORY_POSITION_AWARE_INVERTED;

    public MouseTweaksWheelScrollDirection next() {
        MouseTweaksWheelScrollDirection[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean isInverted() {
        return this == INVERTED || this == INVENTORY_POSITION_AWARE_INVERTED;
    }

    public boolean isPositionAware() {
        return this == INVENTORY_POSITION_AWARE || this == INVENTORY_POSITION_AWARE_INVERTED;
    }
}
