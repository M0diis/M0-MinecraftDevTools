package me.m0dii.modules.freelook;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.modules.freecam.CameraEntity;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Freelook: while the key is held (or toggled), the mouse orbits a third-person camera around you and your
 * player keeps looking (and walking) where it was.
 */
public class FreelookModule extends Module implements PersistentSettings {
    public static final FreelookModule INSTANCE = new FreelookModule();

    private boolean active;
    private float yaw;
    private float pitch;
    private CameraType savedCamera;
    private boolean toggleMode;
    private boolean invertPitch;

    private FreelookModule() {
        super("freelook", "Freelook", false);
    }

    @Override
    public void register() {
        registerHeldKeybind(KeybindCatalog.FREELOOK.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.FREELOOK.defaultKey(),
                client -> {
                    if (toggleMode) {
                        if (active) {
                            stop(client);
                        } else {
                            start(client);
                        }
                    } else if (!active) {
                        start(client);
                    }
                },
                client -> {
                    if (!toggleMode) {
                        stop(client);
                    }
                });
    }

    /** True while the camera is detached from the player's view direction. */
    public static boolean isActive() {
        return INSTANCE.active && !FreecamModule.INSTANCE.isEnabled() && !CameraEntity.hasController();
    }

    public static float yaw() {
        return INSTANCE.yaw;
    }

    public static float pitch() {
        return INSTANCE.pitch;
    }

    /** Mouse movement while active, called instead of turning the player (same 0.15 scale as vanilla). */
    public static void turn(double dx, double dy) {
        INSTANCE.yaw += (float) (dx * 0.15);
        INSTANCE.pitch = Math.clamp(INSTANCE.pitch + (float) (dy * 0.15) * (INSTANCE.invertPitch ? -1 : 1), -90.0f, 90.0f);
    }

    private void start(Minecraft client) {
        if (client.player == null || client.gui.screen() != null || FreecamModule.INSTANCE.isEnabled()) {
            return;
        }
        active = true;
        yaw = client.player.getYRot();
        pitch = client.player.getXRot();
        savedCamera = client.options.getCameraType();
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    private void stop(Minecraft client) {
        if (!active) {
            return;
        }
        active = false;
        if (savedCamera != null) {
            client.options.setCameraType(savedCamera);
            savedCamera = null;
        }
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Key: " + (getKeyBinding() != null ? getKeyBinding().saveString() : "None"));
        settings.add("Mode: " + (toggleMode ? "Toggle" : "Hold"));
        settings.add("Invert pitch: " + (invertPitch ? "ON" : "OFF"));
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 1 -> {
                toggleMode = !toggleMode;
                stop(getClient());
            }
            case 2 -> invertPitch = !invertPitch;
            default -> {
                return;
            }
        }
        if (getClient().player != null) {
            getClient().player.sendOverlayMessage(Component.literal("Freelook: " + (toggleMode ? "toggle" : "hold")
                    + (invertPitch ? ", inverted pitch" : "")));
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("toggle", toggleMode);
        json.addProperty("invertPitch", invertPitch);
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        toggleMode = PersistentSettings.readBoolean(settings, "toggle", false);
        invertPitch = PersistentSettings.readBoolean(settings, "invertPitch", false);
    }
}
