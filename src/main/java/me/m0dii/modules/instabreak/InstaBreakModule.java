package me.m0dii.modules.instabreak;

import me.m0dii.modules.Module;

public class InstaBreakModule extends Module {

    public static final InstaBreakModule INSTANCE = new InstaBreakModule();

    private InstaBreakModule() {
        super("instant_break", "Instant break", false);
    }
}
