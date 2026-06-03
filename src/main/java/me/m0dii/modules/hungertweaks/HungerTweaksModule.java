package me.m0dii.modules.hungertweaks;

import me.m0dii.modules.Module;
import me.m0dii.modules.hungertweaks.network.HungerTweaksClientSyncHandler;
import me.m0dii.utils.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HungerTweaksModule extends Module {
    public static final HungerTweaksModule INSTANCE = new HungerTweaksModule();

    private static final float DEFAULT_MAX_FLASH_ALPHA = 0.65f;
    private static final float STEP = 0.05f;

    private HungerTweaksModule() {
        super("hunger_tweaks", "Hunger Tweaks", ModConfig.hungerTweaksModuleEnabled);
    }

    @Override
    public void register() {
        HungerTweaksHUDOverlayHandler.init();
        HungerTweaksTooltipOverlayHandler.init();
        HungerTweaksDebugInfoHandler.init();
        HungerTweaksClientSyncHandler.init();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.hungerTweaksModuleEnabled = this.enabled);
    }

    @Override
    protected void onDisable() {
        HungerTweaksHUDOverlayHandler handler = HungerTweaksHUDOverlayHandler.INSTANCE;
        if (handler != null) {
            handler.resetFlash();
        }
    }

    public boolean showFoodValuesInTooltip() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodValuesInTooltip;
    }

    public boolean showFoodValuesInTooltipAlways() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodValuesInTooltipAlways;
    }

    public boolean showSaturationHudOverlay() {
        return isEnabled() && ModConfig.hungerTweaksShowSaturationHudOverlay;
    }

    public boolean showFoodValuesHudOverlay() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodValuesHudOverlay;
    }

    public boolean showFoodValuesHudOverlayWhenOffhand() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand;
    }

    public boolean showFoodExhaustionHudUnderlay() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay;
    }

    public boolean showFoodHealthHudOverlay() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodHealthHudOverlay;
    }

    public boolean showFoodDebugInfo() {
        return isEnabled() && ModConfig.hungerTweaksShowFoodDebugInfo;
    }

    public boolean showVanillaAnimationsOverlay() {
        return isEnabled() && ModConfig.hungerTweaksShowVanillaAnimationsOverlay;
    }

    public float getMaxHudOverlayFlashAlpha() {
        return clampFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha);
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + onOff(isEnabled()));
        settings.add("Tooltip Food Values: " + onOff(ModConfig.hungerTweaksShowFoodValuesInTooltip));
        settings.add("Tooltip Always Visible: " + onOff(ModConfig.hungerTweaksShowFoodValuesInTooltipAlways));
        settings.add("Saturation Overlay: " + onOff(ModConfig.hungerTweaksShowSaturationHudOverlay));
        settings.add("Held Food Overlay: " + onOff(ModConfig.hungerTweaksShowFoodValuesHudOverlay));
        settings.add("Offhand Overlay: " + onOff(ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand));
        settings.add("Exhaustion Underlay: " + onOff(ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay));
        settings.add("Estimated Health Overlay: " + onOff(ModConfig.hungerTweaksShowFoodHealthHudOverlay));
        settings.add("Debug HUD Food Info: " + onOff(ModConfig.hungerTweaksShowFoodDebugInfo));
        settings.add("Match Vanilla Animation: " + onOff(ModConfig.hungerTweaksShowVanillaAnimationsOverlay));
        settings.add("Max Flash Alpha: " + formatFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha));
        settings.add("Max Flash Alpha (+0.05)");
        settings.add("Max Flash Alpha (-0.05)");
        settings.add("Reset Max Flash Alpha");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltip = !ModConfig.hungerTweaksShowFoodValuesInTooltip);
            case 2 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesInTooltipAlways = !ModConfig.hungerTweaksShowFoodValuesInTooltipAlways);
            case 3 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowSaturationHudOverlay = !ModConfig.hungerTweaksShowSaturationHudOverlay);
            case 4 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlay = !ModConfig.hungerTweaksShowFoodValuesHudOverlay);
            case 5 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand = !ModConfig.hungerTweaksShowFoodValuesHudOverlayWhenOffhand);
            case 6 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay = !ModConfig.hungerTweaksShowFoodExhaustionHudUnderlay);
            case 7 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodHealthHudOverlay = !ModConfig.hungerTweaksShowFoodHealthHudOverlay);
            case 8 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowFoodDebugInfo = !ModConfig.hungerTweaksShowFoodDebugInfo);
            case 9 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksShowVanillaAnimationsOverlay = !ModConfig.hungerTweaksShowVanillaAnimationsOverlay);
            case 11 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksMaxHudOverlayFlashAlpha = clampFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha + STEP));
            case 12 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksMaxHudOverlayFlashAlpha = clampFlashAlpha(ModConfig.hungerTweaksMaxHudOverlayFlashAlpha - STEP));
            case 13 -> ModConfig.updateAndSave(() -> ModConfig.hungerTweaksMaxHudOverlayFlashAlpha = DEFAULT_MAX_FLASH_ALPHA);
            default -> {
            }
        }
    }

    public static float clampFlashAlpha(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }

    public static String formatFlashAlpha(float value) {
        return String.format(Locale.ROOT, "%.2f", clampFlashAlpha(value));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
