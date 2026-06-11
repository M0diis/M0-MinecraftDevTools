package me.m0dii.modules.reach;

import me.m0dii.modules.Module;
import me.m0dii.modules.optin.RestrictedModuleOptInNetworking;
import me.m0dii.utils.ModConfig;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReachModule extends Module {

    public static final ReachModule INSTANCE = new ReachModule();

    private static final double MIN_REACH = 1.0;
    private static final double MAX_REACH = 16.0;
    private static final double MAX_MULTIPLAYER_EXTRA = 4.0;
    private static final double STEP = 0.25;

    private ReachModule() {
        super("reach", "Reach", ModConfig.reachModuleEnabled);
    }

    @Override
    public boolean requiresServerSideOptIn() {
        return true;
    }

    @Override
    protected Identifier getRequiredServerOptInChannel() {
        return RestrictedModuleOptInNetworking.REACH_CHANNEL_ID;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.reachModuleEnabled = this.enabled);
    }

    public double applyBlockReach(double vanillaReach, boolean singleplayerLike) {
        return applyReach(vanillaReach, getBlockReachDistance(), getMultiplayerBlockExtra(), singleplayerLike);
    }

    public double applyEntityReach(double vanillaReach, boolean singleplayerLike) {
        return applyReach(vanillaReach, getEntityReachDistance(), getMultiplayerEntityExtra(), singleplayerLike);
    }

    public boolean safeMultiplayerClamp() {
        return ModConfig.reachSafeMultiplayerClamp;
    }

    public double getBlockReachDistance() {
        return Math.clamp(ModConfig.reachBlockDistance, MIN_REACH, MAX_REACH);
    }

    public double getEntityReachDistance() {
        return Math.clamp(ModConfig.reachEntityDistance, MIN_REACH, MAX_REACH);
    }

    public double getMultiplayerBlockExtra() {
        return Math.clamp(ModConfig.reachMultiplayerBlockExtra, 0.0, MAX_MULTIPLAYER_EXTRA);
    }

    public double getMultiplayerEntityExtra() {
        return Math.clamp(ModConfig.reachMultiplayerEntityExtra, 0.0, MAX_MULTIPLAYER_EXTRA);
    }

    private double applyReach(double vanillaReach, double configuredReach, double multiplayerExtra, boolean singleplayerLike) {
        double vanilla = Math.max(MIN_REACH, vanillaReach);
        double configured = Math.clamp(configuredReach, MIN_REACH, MAX_REACH);
        if (configured <= vanilla) {
            return configured;
        }

        if (singleplayerLike || !safeMultiplayerClamp()) {
            return configured;
        }

        return Math.min(configured, vanilla + Math.clamp(multiplayerExtra, 0.0, MAX_MULTIPLAYER_EXTRA));
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + onOff(isEnabled()));
        settings.add("Block Reach: " + format(getBlockReachDistance()));
        settings.add("Block Reach (+0.25)");
        settings.add("Block Reach (-0.25)");
        settings.add("Entity Reach: " + format(getEntityReachDistance()));
        settings.add("Entity Reach (+0.25)");
        settings.add("Entity Reach (-0.25)");
        settings.add("Safe Multiplayer Clamp: " + onOff(safeMultiplayerClamp()));
        settings.add("MP Block Extra: " + format(getMultiplayerBlockExtra()));
        settings.add("MP Block Extra (+0.25)");
        settings.add("MP Block Extra (-0.25)");
        settings.add("MP Entity Extra: " + format(getMultiplayerEntityExtra()));
        settings.add("MP Entity Extra (+0.25)");
        settings.add("MP Entity Extra (-0.25)");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 2 -> ModConfig.updateAndSave(() -> ModConfig.reachBlockDistance = clampReach(ModConfig.reachBlockDistance + STEP));
            case 3 -> ModConfig.updateAndSave(() -> ModConfig.reachBlockDistance = clampReach(ModConfig.reachBlockDistance - STEP));
            case 5 -> ModConfig.updateAndSave(() -> ModConfig.reachEntityDistance = clampReach(ModConfig.reachEntityDistance + STEP));
            case 6 -> ModConfig.updateAndSave(() -> ModConfig.reachEntityDistance = clampReach(ModConfig.reachEntityDistance - STEP));
            case 7 -> ModConfig.updateAndSave(() -> ModConfig.reachSafeMultiplayerClamp = !ModConfig.reachSafeMultiplayerClamp);
            case 9 -> ModConfig.updateAndSave(() -> ModConfig.reachMultiplayerBlockExtra = clampExtra(ModConfig.reachMultiplayerBlockExtra + STEP));
            case 10 -> ModConfig.updateAndSave(() -> ModConfig.reachMultiplayerBlockExtra = clampExtra(ModConfig.reachMultiplayerBlockExtra - STEP));
            case 12 -> ModConfig.updateAndSave(() -> ModConfig.reachMultiplayerEntityExtra = clampExtra(ModConfig.reachMultiplayerEntityExtra + STEP));
            case 13 -> ModConfig.updateAndSave(() -> ModConfig.reachMultiplayerEntityExtra = clampExtra(ModConfig.reachMultiplayerEntityExtra - STEP));
            default -> {
            }
        }
    }

    private static double clampReach(double value) {
        return Math.clamp(value, MIN_REACH, MAX_REACH);
    }

    private static double clampExtra(double value) {
        return Math.clamp(value, 0.0, MAX_MULTIPLAYER_EXTRA);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
