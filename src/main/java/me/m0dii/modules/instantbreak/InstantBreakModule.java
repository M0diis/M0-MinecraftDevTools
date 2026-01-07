package me.m0dii.modules.instantbreak;

import me.m0dii.modules.Module;

public class InstantBreakModule extends Module {

    public static final InstantBreakModule INSTANCE = new InstantBreakModule();

    private InstantBreakModule() {
        super("instant_break", "Instant Break", false);
    }
}
