package me.m0dii.modules.commandblockui;

import me.m0dii.modules.Module;

public final class BetterCommandBlockUiModule extends Module {

    public static final BetterCommandBlockUiModule INSTANCE = new BetterCommandBlockUiModule();

    private BetterCommandBlockUiModule() {
        super("better_command_block_ui", "Better Command Block UI", true);
    }

    @Override
    public boolean hasSettings() {
        return false;
    }
}
