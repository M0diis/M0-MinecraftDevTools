package me.m0dii.modules.fullbright;

import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FullbrightModule extends Module {

    public static final FullbrightModule INSTANCE = new FullbrightModule();

    @Getter
    private double gammaValue = ModConfig.fullbrightGamma;

    protected FullbrightModule() {
        super("fullbright", "Fullbright", false);
    }

    @Override
    public void register() {
        registerPressedKeybind(
                "key.m0-dev-tools.fullbright",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                client -> toggleEnabled()
        );
    }


    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Current Gamma: " + String.format("%.1f", gammaValue));
        settings.add("Increase Gamma (+1)");
        settings.add("Increase Gamma (+5)");
        settings.add("Decrease Gamma (-1)");
        settings.add("Decrease Gamma (-5)");
        settings.add("Reset to Default (100)");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 1 -> updateGamma(gammaValue + 1);
            case 2 -> updateGamma(gammaValue + 5);
            case 3 -> updateGamma(gammaValue - 1);
            case 4 -> updateGamma(gammaValue - 5);
            case 5 -> updateGamma(100);
            default -> {
                return;
            }
        }

        ModConfig.updateAndSave(() -> ModConfig.fullbrightGamma = gammaValue);
    }

    private void updateGamma(double newGamma) {
        gammaValue = Math.clamp(newGamma, 0.0, 100.0);
    }
}
