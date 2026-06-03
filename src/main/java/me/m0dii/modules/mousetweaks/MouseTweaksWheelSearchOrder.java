package me.m0dii.modules.mousetweaks;

public enum MouseTweaksWheelSearchOrder {
    FIRST_TO_LAST,
    LAST_TO_FIRST;

    public MouseTweaksWheelSearchOrder next() {
        return this == FIRST_TO_LAST ? LAST_TO_FIRST : FIRST_TO_LAST;
    }
}
