package me.m0dii.modules.worldedit;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;

public final class WorldEditModule extends Module {
    public static final WorldEditModule INSTANCE = new WorldEditModule();

    private WorldEditModule() {
        super("worldedit", "WorldEdit", ModConfig.worldEditModuleEnabled);
    }

    @Override
    public void register() {
        WorldEditClientCommands.register();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.worldEditModuleEnabled = this.enabled);
    }
}
