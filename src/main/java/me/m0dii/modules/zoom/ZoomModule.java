package me.m0dii.modules.zoom;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ZoomModule extends Module implements me.m0dii.modules.NumericSetting.Provider {

    public static final ZoomModule INSTANCE = new ZoomModule();

    @Getter
    public float zoomFov = 30f;

    @Getter
    public boolean held = false;

    protected ZoomModule() {
        super("zoom", "Zoom", false);
    }

    @Override
    public void register() {
        registerHeldKeybind(
                KeybindCatalog.ZOOM_HOLD.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.ZOOM_HOLD.defaultKey(),
                client -> held = true,
                client -> held = false
        );
    }

    @Override
    public List<me.m0dii.modules.NumericSetting> numericSettings() {
        return List.of(me.m0dii.modules.NumericSetting.of(0, "Zoom FOV", 5, 110, 1, 0, () -> zoomFov, v -> zoomFov = (float) v, 1, 2));
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Zoom FOV: " + String.format("%.1f", zoomFov));
        settings.add("Decrease FOV (-5)");
        settings.add("Increase FOV (+5)");
        settings.add("Reset to Default (30)");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 1 -> {
                zoomFov = Math.max(5f, zoomFov - 5f);
                if (getClient().player != null) {
                    getClient().player.sendOverlayMessage(Component.literal("Zoom FOV: " + String.format("%.1f", zoomFov)));
                }
            }
            case 2 -> {
                zoomFov = Math.min(110f, zoomFov + 5f);
                if (getClient().player != null) {
                    getClient().player.sendOverlayMessage(Component.literal("Zoom FOV: " + String.format("%.1f", zoomFov)));
                }
            }
            case 3 -> {
                zoomFov = 30f;
                if (getClient().player != null) {
                    getClient().player.sendOverlayMessage(Component.literal("Zoom FOV reset to 30"));
                }
            }
            default -> {
                // Do nothing
            }
        }
    }

    public void onScroll(double horizontal, double vertical) {
        if (held) {
            if (vertical > 0) {
                zoomFov -= 5f;
                if (zoomFov < 5f) {
                    zoomFov = 5f;
                }
            } else if (vertical < 0) {
                zoomFov += 5f;
                if (zoomFov > 90f) {
                    zoomFov = 90f;
                }
            }

            getClient().player.sendOverlayMessage(Component.literal("Zoom FOV: " + (int) zoomFov));
        }
    }
}
