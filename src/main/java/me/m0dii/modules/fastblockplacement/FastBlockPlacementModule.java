package me.m0dii.modules.fastblockplacement;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;

public class FastBlockPlacementModule extends Module {

    public static final FastBlockPlacementModule INSTANCE = new FastBlockPlacementModule();

    private FastBlockPlacementModule() {
        super("fast_block_placement", "Fast Block Placement",
                ModConfig.fastBlockPlacementEnabled || ModConfig.tweaksFastBlockPlacement);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> {
            ModConfig.fastBlockPlacementEnabled = this.enabled;
            ModConfig.tweaksFastBlockPlacement = this.enabled;
        });
    }

    @Override
    public boolean hasSettings() {
        return false;
    }
}
