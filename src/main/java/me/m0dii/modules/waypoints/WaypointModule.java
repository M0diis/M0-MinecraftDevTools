package me.m0dii.modules.waypoints;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;

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
}
