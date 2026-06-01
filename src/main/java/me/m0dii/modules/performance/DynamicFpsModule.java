package me.m0dii.modules.performance;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;

public class DynamicFpsModule extends Module {

    public static final DynamicFpsModule INSTANCE = new DynamicFpsModule();

    private DynamicFpsModule() {
        super("dynamic_fps", "Dynamic FPS", ModConfig.dynamicFpsEnabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.dynamicFpsEnabled = this.enabled);
    }

    @Override
    public boolean hasSettings() {
        return false;
    }

    public int getUnfocusedFpsLimit() {
        return Math.clamp(ModConfig.dynamicFpsUnfocusedFps, 5, 260);
    }
}
