package me.m0dii.modules.watson;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side CoreProtect visualizer inspired by Watson-style inspection overlays.
 */
public final class WatsonCoreProtectModule extends Module {
    public static final WatsonCoreProtectModule INSTANCE = new WatsonCoreProtectModule();

    private WatsonCoreProtectModule() {
        super("watson_coreprotect", "Watson CP", false);
    }

    @Override
    public void register() {
        WatsonCommands.register();
        CoreProtectRenderer.register();

        registerPressedKeybind(
                "key.m0-dev-tools.toggle_watson_cp",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                client -> toggleEnabled()
        );
    }

    @Override
    protected void onDisable() {
        // Keep entries by default so players can toggle renderer without losing recent context.
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Entries: " + CoreProtectTracker.size());
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("Tracers: " + (CoreProtectRenderer.isTracersEnabled() ? "ON" : "OFF"));
        settings.add("Vectors: " + (CoreProtectRenderer.isVectorsEnabled() ? "ON" : "OFF"));
        settings.add("Labels: " + (CoreProtectRenderer.isLabelsEnabled() ? "ON" : "OFF"));
        settings.add("Outline Width: " + String.format("%.1f", CoreProtectRenderer.getOutlineLineWidth()));
        settings.add("Outline Color: " + CoreProtectRenderer.getOutlineColorPresetName());
        settings.add("Vector Width: " + String.format("%.1f", CoreProtectRenderer.getVectorLineWidth()));
        settings.add("Vector Color: " + CoreProtectRenderer.getVectorColorPresetName());
        settings.add("TTL(s): " + (CoreProtectTracker.getTtlMs() / 1000L));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        if (settingIndex == 1) {
            toggleEnabled();
            return;
        }

        if (settingIndex == 2) {
            CoreProtectRenderer.setTracersEnabled(!CoreProtectRenderer.isTracersEnabled());
            return;
        }

        if (settingIndex == 3) {
            CoreProtectRenderer.setVectorsEnabled(!CoreProtectRenderer.isVectorsEnabled());
            return;
        }

        if (settingIndex == 4) {
            CoreProtectRenderer.setLabelsEnabled(!CoreProtectRenderer.isLabelsEnabled());
            return;
        }

        if (settingIndex == 5) {
            float current = CoreProtectRenderer.getOutlineLineWidth();
            float next = current >= 5.0f ? 1.0f : current + 0.5f;
            CoreProtectRenderer.setOutlineLineWidth(next);
            return;
        }

        if (settingIndex == 6) {
            CoreProtectRenderer.cycleOutlineColorPreset();
            return;
        }

        if (settingIndex == 7) {
            float current = CoreProtectRenderer.getVectorLineWidth();
            float next = current >= 5.0f ? 1.0f : current + 0.5f;
            CoreProtectRenderer.setVectorLineWidth(next);
            return;
        }

        if (settingIndex == 8) {
            CoreProtectRenderer.cycleVectorColorPreset();
            return;
        }

        if (settingIndex == 9) {
            // Cycle quickly between 30s, 60s, 120s, 300s for UI convenience.
            long current = CoreProtectTracker.getTtlMs() / 1000L;
            int next = switch ((int) current) {
                case 30 -> 60;
                case 60 -> 120;
                case 120 -> 300;
                default -> 30;
            };
            CoreProtectTracker.setTtlSeconds(next);
        }
    }
}

