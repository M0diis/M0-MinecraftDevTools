package me.m0dii.modules.zoom;

import lombok.Getter;
import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ZoomModule extends Module {

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
                "key.m0-dev-tools.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                client -> held = true,
                client -> held = false
        );
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
                    getClient().player.sendMessage(Text.literal("Zoom FOV: " + String.format("%.1f", zoomFov)), true);
                }
            }
            case 2 -> {
                zoomFov = Math.min(110f, zoomFov + 5f);
                if (getClient().player != null) {
                    getClient().player.sendMessage(Text.literal("Zoom FOV: " + String.format("%.1f", zoomFov)), true);
                }
            }
            case 3 -> {
                zoomFov = 30f;
                if (getClient().player != null) {
                    getClient().player.sendMessage(Text.literal("Zoom FOV reset to 30"), true);
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

            getClient().player.sendMessage(Text.literal("Zoom FOV: " + (int) zoomFov), true);
        }
    }
}
