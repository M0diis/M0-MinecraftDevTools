package me.m0dii.modules.bridging;

import net.minecraft.util.math.Direction;

public enum BridgingAxisMode {
    HORIZONTAL,
    VERTICAL,
    BOTH;

    public boolean supports(Direction direction) {
        return switch (this) {
            case HORIZONTAL -> direction.getAxis().isHorizontal();
            case VERTICAL -> direction.getAxis().isVertical();
            case BOTH -> true;
        };
    }

    public BridgingAxisMode next() {
        BridgingAxisMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
