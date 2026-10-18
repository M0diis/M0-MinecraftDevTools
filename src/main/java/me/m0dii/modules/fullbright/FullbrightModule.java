package me.m0dii.modules.fullbright;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import me.m0dii.utils.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class FullbrightModule extends Module implements me.m0dii.modules.NumericSetting.Provider {

    public static final FullbrightModule INSTANCE = new FullbrightModule();

    @Getter
    private double gammaValue = ModConfig.fullbrightGamma;

    protected FullbrightModule() {
        super("fullbright", "Fullbright", false);
    }

    @Override
    public void register() {
        registerPressedKeybind(
                KeybindCatalog.FULLBRIGHT_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.FULLBRIGHT_TOGGLE.defaultKey(),
                client -> toggleEnabled()
        );
    }


    @Override
    public List<me.m0dii.modules.NumericSetting> numericSettings() {
        return List.of(me.m0dii.modules.NumericSetting.of(0, "Gamma", 0, 100, 1, 0, () -> gammaValue, v -> {
            updateGamma(v);
            ModConfig.updateAndSave(() -> ModConfig.fullbrightGamma = gammaValue);
        }, 1, 2, 3, 4));
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
