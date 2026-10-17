package me.m0dii.modules.waypoints;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.modules.PersistentSettings;
import me.m0dii.utils.KeybindCatalog;

import java.util.ArrayList;
import java.util.List;

public class WaypointModule extends Module implements PersistentSettings {
    public static final WaypointModule INSTANCE = new WaypointModule();

    protected WaypointModule() {
        super("waypoints", "Waypoints", false);
    }

    @Override
    public void register() {
        WaypointCommands.register();
        WaypointHandler.register();
        WaypointRenderer.register();
        WaypointHud.register();

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_ADD.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.WAYPOINT_ADD.defaultKey(),
                (minecraftClient -> WaypointHandler.addWaypoint())
        );

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_LIST.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.WAYPOINT_LIST.defaultKey(),
                (minecraftClient -> WaypointHandler.listWaypoints())
        );

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_GUI.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.WAYPOINT_GUI.defaultKey(),
                (minecraftClient -> {
                    if (minecraftClient.gui.screen() == null) {
                        minecraftClient.gui.setScreen(WaypointScreen.create(null));
                    }
                })
        );

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_RENDER_TOGGLE.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.WAYPOINT_RENDER_TOGGLE.defaultKey(),
                (minecraftClient -> {
                    WaypointRenderer.setEnabled(!WaypointRenderer.isEnabled());
                    if (minecraftClient.player != null) {
                        String status = WaypointRenderer.isEnabled() ? "enabled" : "disabled";
                        minecraftClient.player.sendOverlayMessage(
                                net.minecraft.network.chat.Component.literal("Waypoint rendering " + status)
                                        .withStyle(WaypointRenderer.isEnabled()
                                                ? net.minecraft.ChatFormatting.GREEN
                                                : net.minecraft.ChatFormatting.RED));
                    }
                })
        );
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Show waypoints: " + (WaypointRenderer.isEnabled() ? "ON" : "OFF"));
        settings.add("Beams: " + (WaypointRenderer.isBeams() ? "ON" : "OFF"));
        settings.add("Show other dimension (×8 / ÷8): " + (WaypointRenderer.isShowOtherDimension() ? "ON" : "OFF"));
        settings.add("Track nearest waypoint");
        settings.add("Stop tracking");
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> WaypointRenderer.setEnabled(!WaypointRenderer.isEnabled());
            case 1 -> WaypointRenderer.setBeams(!WaypointRenderer.isBeams());
            case 2 -> WaypointRenderer.setShowOtherDimension(!WaypointRenderer.isShowOtherDimension());
            case 3 -> WaypointHandler.track("nearest");
            case 4 -> WaypointHandler.track("off");
            default -> {
                // nothing
            }
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject json = new JsonObject();
        json.addProperty("render", WaypointRenderer.isEnabled());
        json.addProperty("beams", WaypointRenderer.isBeams());
        json.addProperty("showOtherDimension", WaypointRenderer.isShowOtherDimension());
        return json;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        WaypointRenderer.setEnabled(PersistentSettings.readBoolean(settings, "render", WaypointRenderer.isEnabled()));
        WaypointRenderer.setBeams(PersistentSettings.readBoolean(settings, "beams", WaypointRenderer.isBeams()));
        WaypointRenderer.setShowOtherDimension(PersistentSettings.readBoolean(settings, "showOtherDimension",
                WaypointRenderer.isShowOtherDimension()));
    }
}
