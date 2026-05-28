package me.m0dii.modules.waypoints;

import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.util.InputUtil;

public class WaypointModule extends Module {
    public static final WaypointModule INSTANCE = new WaypointModule();

    protected WaypointModule() {
        super("waypoints", "Waypoints", false);
    }

    @Override
    public void register() {
        WaypointCommands.register();
        WaypointHandler.register();
        WaypointRenderer.register();

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_ADD.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.WAYPOINT_ADD.defaultKey(),
                (minecraftClient -> WaypointHandler.addWaypoint())
        );

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_LIST.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.WAYPOINT_LIST.defaultKey(),
                (minecraftClient -> WaypointHandler.listWaypoints())
        );

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_GUI.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.WAYPOINT_GUI.defaultKey(),
                (minecraftClient -> {
                    if (minecraftClient.currentScreen == null) {
                        minecraftClient.setScreen(WaypointScreen.create(null));
                    }
                })
        );

        registerPressedKeybind(
                KeybindCatalog.WAYPOINT_RENDER_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.WAYPOINT_RENDER_TOGGLE.defaultKey(),
                (minecraftClient -> {
                    WaypointRenderer.setEnabled(!WaypointRenderer.isEnabled());
                    if (minecraftClient.player != null) {
                        String status = WaypointRenderer.isEnabled() ? "enabled" : "disabled";
                        minecraftClient.player.sendMessage(
                                net.minecraft.text.Text.literal("Waypoint rendering " + status)
                                        .formatted(WaypointRenderer.isEnabled()
                                                ? net.minecraft.util.Formatting.GREEN
                                                : net.minecraft.util.Formatting.RED),
                                true
                        );
                    }
                })
        );
    }
}
