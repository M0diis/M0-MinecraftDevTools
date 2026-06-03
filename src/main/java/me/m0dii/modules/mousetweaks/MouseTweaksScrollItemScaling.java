package me.m0dii.modules.mousetweaks;

public enum MouseTweaksScrollItemScaling {
    PROPORTIONAL,
    ALWAYS_ONE;

    public MouseTweaksScrollItemScaling next() {
        return this == PROPORTIONAL ? ALWAYS_ONE : PROPORTIONAL;
    }

    public double scale(double scrollDelta) {
        return this == PROPORTIONAL ? scrollDelta : Math.signum(scrollDelta);
    }
}
