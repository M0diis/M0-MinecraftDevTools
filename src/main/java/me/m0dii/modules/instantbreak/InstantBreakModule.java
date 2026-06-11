package me.m0dii.modules.instantbreak;

import me.m0dii.modules.Module;
import me.m0dii.modules.optin.RestrictedModuleOptInNetworking;
import net.minecraft.util.Identifier;

public class InstantBreakModule extends Module {

    public static final InstantBreakModule INSTANCE = new InstantBreakModule();

    private InstantBreakModule() {
        super("instant_break", "Instant Break", false);
    }

    @Override
    public boolean requiresServerSideOptIn() {
        return true;
    }

    @Override
    protected Identifier getRequiredServerOptInChannel() {
        return RestrictedModuleOptInNetworking.INSTANT_BREAK_CHANNEL_ID;
    }
}
