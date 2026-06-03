package me.m0dii.modules.bridging;

import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BridgingTweaksModule extends Module {
    public static final BridgingTweaksModule INSTANCE = new BridgingTweaksModule();

    private final BridgingTweaksRenderer renderer = new BridgingTweaksRenderer();

    private BridgingTweaksModule() {
        super("bridging_tweaks", "Bridging Tweaks", ModConfig.bridgingTweaksEnabled);
    }

    @Override
    public void register() {
        this.renderer.register();
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(M0DevToolsClient.MOD_ID, "bridging_tweaks_hud"),
                this.renderer::onHudRender
        );
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.bridgingTweaksEnabled = this.enabled);
    }

    public boolean onlyBridgeWhenCrouched() {
        return isEnabled() && ModConfig.bridgingOnlyWhenCrouched;
    }

    public boolean showCrosshair() {
        return ModConfig.bridgingShowCrosshair;
    }

    public boolean showOutline() {
        return ModConfig.bridgingShowOutline;
    }

    public boolean showOutlineWhenNotBridging() {
        return ModConfig.bridgingShowOutlineWhenNotBridging;
    }

    public boolean nonBridgeOutlineRespectsCrouchRules() {
        return ModConfig.bridgingNonBridgeRespectsCrouchRules;
    }

    public boolean allowReplaceableBlocks() {
        return ModConfig.bridgingEnableNonSolidReplace;
    }

    public boolean skipTorchBridging() {
        return ModConfig.bridgingSkipTorchBridging;
    }

    public boolean slabAssist() {
        return ModConfig.bridgingEnableSlabAssist;
    }

    public int delayPostBridging() {
        return Math.max(0, ModConfig.bridgingDelayPostBridging);
    }

    public boolean debugHighlight() {
        return ModConfig.bridgingShowDebugHighlight;
    }

    public boolean debugNonBridgingHighlight() {
        return ModConfig.bridgingShowDebugNonBridgingHighlight;
    }

    public boolean debugTrace() {
        return ModConfig.bridgingShowDebugTrace;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + onOff(isEnabled()));
        settings.add("Min Distance: " + formatPercent(ModConfig.bridgingMinBridgeDistance));
        settings.add("Only When Crouched: " + onOff(ModConfig.bridgingOnlyWhenCrouched));
        settings.add("Bridge Axes: " + ModConfig.bridgingSupportedAxes);
        settings.add("Crouched Axes: " + ModConfig.bridgingSupportedAxesWhenCrouched);
        settings.add("Post Delay: " + ModConfig.bridgingDelayPostBridging + "t");
        settings.add("Show Crosshair: " + onOff(ModConfig.bridgingShowCrosshair));
        settings.add("Show Outline: " + onOff(ModConfig.bridgingShowOutline));
        settings.add("Outline When Not Bridging: " + onOff(ModConfig.bridgingShowOutlineWhenNotBridging));
        settings.add("Outline Respects Crouch: " + onOff(ModConfig.bridgingNonBridgeRespectsCrouchRules));
        settings.add("Outline Color: " + formatColor(ModConfig.bridgingOutlineColor));
        settings.add("Skip Torch Blocks: " + onOff(ModConfig.bridgingSkipTorchBridging));
        settings.add("Slab Assist: " + onOff(ModConfig.bridgingEnableSlabAssist));
        settings.add("Replaceable Targets: " + onOff(ModConfig.bridgingEnableNonSolidReplace));
        settings.add("Snap Strength: " + formatFloat(ModConfig.bridgingSnapStrength));
        settings.add("Adjacency: " + ModConfig.bridgingAdjacency);
        settings.add("Perspective Lock: " + ModConfig.bridgingPerspectiveLock);
        settings.add("Debug Highlight: " + onOff(ModConfig.bridgingShowDebugHighlight));
        settings.add("Debug Non-Bridge: " + onOff(ModConfig.bridgingShowDebugNonBridgingHighlight));
        settings.add("Debug Trace: " + onOff(ModConfig.bridgingShowDebugTrace));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> ModConfig.updateAndSave(() -> ModConfig.bridgingMinBridgeDistance = wrapPercent(ModConfig.bridgingMinBridgeDistance + 5.0f));
            case 2 -> ModConfig.updateAndSave(() -> ModConfig.bridgingOnlyWhenCrouched = !ModConfig.bridgingOnlyWhenCrouched);
            case 3 -> ModConfig.updateAndSave(() -> ModConfig.bridgingSupportedAxes = ModConfig.bridgingSupportedAxes.next());
            case 4 -> ModConfig.updateAndSave(() -> ModConfig.bridgingSupportedAxesWhenCrouched = ModConfig.bridgingSupportedAxesWhenCrouched.next());
            case 5 -> ModConfig.updateAndSave(() -> ModConfig.bridgingDelayPostBridging = (ModConfig.bridgingDelayPostBridging + 1) % 21);
            case 6 -> ModConfig.updateAndSave(() -> ModConfig.bridgingShowCrosshair = !ModConfig.bridgingShowCrosshair);
            case 7 -> ModConfig.updateAndSave(() -> ModConfig.bridgingShowOutline = !ModConfig.bridgingShowOutline);
            case 8 -> ModConfig.updateAndSave(() -> ModConfig.bridgingShowOutlineWhenNotBridging = !ModConfig.bridgingShowOutlineWhenNotBridging);
            case 9 -> ModConfig.updateAndSave(() -> ModConfig.bridgingNonBridgeRespectsCrouchRules = !ModConfig.bridgingNonBridgeRespectsCrouchRules);
            case 10 -> ModConfig.updateAndSave(() -> ModConfig.bridgingOutlineColor = BridgingTweaksRenderer.nextOutlineColor(ModConfig.bridgingOutlineColor, true));
            case 11 -> ModConfig.updateAndSave(() -> ModConfig.bridgingSkipTorchBridging = !ModConfig.bridgingSkipTorchBridging);
            case 12 -> ModConfig.updateAndSave(() -> ModConfig.bridgingEnableSlabAssist = !ModConfig.bridgingEnableSlabAssist);
            case 13 -> ModConfig.updateAndSave(() -> ModConfig.bridgingEnableNonSolidReplace = !ModConfig.bridgingEnableNonSolidReplace);
            case 14 -> ModConfig.updateAndSave(() -> ModConfig.bridgingSnapStrength = wrapUnit(ModConfig.bridgingSnapStrength + 0.05f));
            case 15 -> ModConfig.updateAndSave(() -> ModConfig.bridgingAdjacency = ModConfig.bridgingAdjacency.next());
            case 16 -> ModConfig.updateAndSave(() -> ModConfig.bridgingPerspectiveLock = ModConfig.bridgingPerspectiveLock.next());
            case 17 -> ModConfig.updateAndSave(() -> ModConfig.bridgingShowDebugHighlight = !ModConfig.bridgingShowDebugHighlight);
            case 18 -> ModConfig.updateAndSave(() -> ModConfig.bridgingShowDebugNonBridgingHighlight = !ModConfig.bridgingShowDebugNonBridgingHighlight);
            case 19 -> ModConfig.updateAndSave(() -> ModConfig.bridgingShowDebugTrace = !ModConfig.bridgingShowDebugTrace);
            default -> {
            }
        }
    }

    static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    static String formatPercent(float value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    static String formatColor(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    static float wrapPercent(float value) {
        return value > 100.0f ? 0.0f : Math.clamp(value, 0.0f, 100.0f);
    }

    static float wrapUnit(float value) {
        if (value > 1.0f) {
            return 0.0f;
        }
        return Math.clamp(value, 0.0f, 1.0f);
    }
}
