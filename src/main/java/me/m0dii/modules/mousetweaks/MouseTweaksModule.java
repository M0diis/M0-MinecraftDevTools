package me.m0dii.modules.mousetweaks;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;

import java.util.ArrayList;
import java.util.List;

public class MouseTweaksModule extends Module {
    public static final MouseTweaksModule INSTANCE = new MouseTweaksModule();

    private MouseTweaksModule() {
        super("mouse_tweaks", "Mouse Tweaks", ModConfig.mouseTweaksModuleEnabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        super.setEnabled(enabled);
        ModConfig.updateAndSave(() -> ModConfig.mouseTweaksModuleEnabled = this.enabled);
    }

    @Override
    protected void onDisable() {
        MouseTweaksRuntime.reset();
    }

    public boolean rmbTweak() {
        return isEnabled() && ModConfig.mouseTweaksRmbTweak;
    }

    public boolean lmbTweakWithItem() {
        return isEnabled() && ModConfig.mouseTweaksLmbTweakWithItem;
    }

    public boolean lmbTweakWithoutItem() {
        return isEnabled() && ModConfig.mouseTweaksLmbTweakWithoutItem;
    }

    public boolean wheelTweak() {
        return isEnabled() && ModConfig.mouseTweaksWheelTweak;
    }

    public MouseTweaksWheelSearchOrder wheelSearchOrder() {
        return ModConfig.mouseTweaksWheelSearchOrder;
    }

    public MouseTweaksWheelScrollDirection wheelScrollDirection() {
        return ModConfig.mouseTweaksWheelScrollDirection;
    }

    public MouseTweaksScrollItemScaling scrollItemScaling() {
        return ModConfig.mouseTweaksScrollItemScaling;
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + onOff(isEnabled()));
        settings.add("RMB Tweak: " + onOff(ModConfig.mouseTweaksRmbTweak));
        settings.add("LMB Tweak With Item: " + onOff(ModConfig.mouseTweaksLmbTweakWithItem));
        settings.add("LMB Tweak Without Item: " + onOff(ModConfig.mouseTweaksLmbTweakWithoutItem));
        settings.add("Wheel Tweak: " + onOff(ModConfig.mouseTweaksWheelTweak));
        settings.add("Wheel Search Order: " + ModConfig.mouseTweaksWheelSearchOrder);
        settings.add("Wheel Scroll Direction: " + ModConfig.mouseTweaksWheelScrollDirection);
        settings.add("Scroll Scaling: " + ModConfig.mouseTweaksScrollItemScaling);
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 1 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksRmbTweak = !ModConfig.mouseTweaksRmbTweak);
            case 2 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithItem = !ModConfig.mouseTweaksLmbTweakWithItem);
            case 3 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksLmbTweakWithoutItem = !ModConfig.mouseTweaksLmbTweakWithoutItem);
            case 4 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelTweak = !ModConfig.mouseTweaksWheelTweak);
            case 5 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelSearchOrder = ModConfig.mouseTweaksWheelSearchOrder.next());
            case 6 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksWheelScrollDirection = ModConfig.mouseTweaksWheelScrollDirection.next());
            case 7 -> ModConfig.updateAndSave(() -> ModConfig.mouseTweaksScrollItemScaling = ModConfig.mouseTweaksScrollItemScaling.next());
            default -> {
            }
        }
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
