package me.m0dii.modules.tweaks;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class TweaksModule extends Module {

    public static final TweaksModule INSTANCE = new TweaksModule();

    private TweaksModule() {
        super("tweaks", "Tweaks", ModConfig.tweaksModuleEnabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.tweaksModuleEnabled = this.enabled);
    }

    public boolean hideOwnEffectParticles() {
        return isEnabled() && ModConfig.tweaksHideOwnEffectParticles;
    }

    public boolean hideOffhandItem() {
        return isEnabled() && ModConfig.tweaksHideOffhandItem;
    }

    public boolean disableBlockBreakingParticles() {
        return isEnabled() && ModConfig.tweaksDisableBlockBreakingParticles;
    }

    public boolean disableEntityRendering() {
        return isEnabled() && ModConfig.tweaksDisableEntityRendering;
    }

    public boolean disableNetherFog() {
        return isEnabled() && ModConfig.tweaksDisableNetherFog;
    }

    public boolean disableRainEffects() {
        return isEnabled() && ModConfig.tweaksDisableRainEffects;
    }

    public boolean disableSounds() {
        return isEnabled() && ModConfig.tweaksDisableSounds;
    }

    public boolean disableWallUnsprint() {
        return isEnabled() && ModConfig.tweaksDisableWallUnsprint;
    }

    public boolean angelBlock() {
        return isEnabled() && ModConfig.tweaksAngelBlock;
    }

    public boolean permanentSneak() {
        return isEnabled() && ModConfig.tweaksPermanentSneak;
    }

    public boolean permanentSprint() {
        return isEnabled() && ModConfig.tweaksPermanentSprint;
    }

    public boolean disableHurtCamera() {
        return isEnabled() && ModConfig.tweaksDisableHurtCamera;
    }

    public boolean disableViewBobbing() {
        return isEnabled() && ModConfig.tweaksDisableViewBobbing;
    }

    public boolean disableRenderDistanceFog() {
        return isEnabled() && ModConfig.tweaksDisableRenderDistanceFog;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + onOff(isEnabled()));
        settings.add("Hide Own Effect Particles: " + onOff(ModConfig.tweaksHideOwnEffectParticles));
        settings.add("Hide Offhand Item: " + onOff(ModConfig.tweaksHideOffhandItem));
        settings.add("Disable Block Breaking Particles: " + onOff(ModConfig.tweaksDisableBlockBreakingParticles));
        settings.add("Disable Entity Rendering: " + onOff(ModConfig.tweaksDisableEntityRendering));
        settings.add("Disable Nether Fog: " + onOff(ModConfig.tweaksDisableNetherFog));
        settings.add("Disable Rain Effects: " + onOff(ModConfig.tweaksDisableRainEffects));
        settings.add("Disable Sounds: " + onOff(ModConfig.tweaksDisableSounds));
        settings.add("Disable Wall Unsprint: " + onOff(ModConfig.tweaksDisableWallUnsprint));
        settings.add("Angel Block: " + onOff(ModConfig.tweaksAngelBlock));
        settings.add("PermanentSneak: " + onOff(ModConfig.tweaksPermanentSneak));
        settings.add("PermanentSprint: " + onOff(ModConfig.tweaksPermanentSprint));
        settings.add("Disable Hurt Camera: " + onOff(ModConfig.tweaksDisableHurtCamera));
        settings.add("Disable View Bobbing: " + onOff(ModConfig.tweaksDisableViewBobbing));
        settings.add("Disable Render Distance Fog: " + onOff(ModConfig.tweaksDisableRenderDistanceFog));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> ModConfig.updateAndSave(() -> ModConfig.tweaksHideOwnEffectParticles = !ModConfig.tweaksHideOwnEffectParticles);
            case 2 -> ModConfig.updateAndSave(() -> ModConfig.tweaksHideOffhandItem = !ModConfig.tweaksHideOffhandItem);
            case 3 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableBlockBreakingParticles = !ModConfig.tweaksDisableBlockBreakingParticles);
            case 4 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableEntityRendering = !ModConfig.tweaksDisableEntityRendering);
            case 5 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableNetherFog = !ModConfig.tweaksDisableNetherFog);
            case 6 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRainEffects = !ModConfig.tweaksDisableRainEffects);
            case 7 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableSounds = !ModConfig.tweaksDisableSounds);
            case 8 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableWallUnsprint = !ModConfig.tweaksDisableWallUnsprint);
            case 9 -> ModConfig.updateAndSave(() -> ModConfig.tweaksAngelBlock = !ModConfig.tweaksAngelBlock);
            case 10 -> ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSneak = !ModConfig.tweaksPermanentSneak);
            case 11 -> ModConfig.updateAndSave(() -> ModConfig.tweaksPermanentSprint = !ModConfig.tweaksPermanentSprint);
            case 12 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableHurtCamera = !ModConfig.tweaksDisableHurtCamera);
            case 13 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableViewBobbing = !ModConfig.tweaksDisableViewBobbing);
            case 14 -> ModConfig.updateAndSave(() -> ModConfig.tweaksDisableRenderDistanceFog = !ModConfig.tweaksDisableRenderDistanceFog);
            default -> {
            }
        }
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
