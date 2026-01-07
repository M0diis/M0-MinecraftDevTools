package me.m0dii.modules.waypoints;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

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
                "key.m0-dev-tools.add_waypoint",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_ADD, // Numpad +
                (minecraftClient -> WaypointHandler.addWaypoint())
        );

        registerPressedKeybind(
                "key.m0-dev-tools.list_waypoints",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_MULTIPLY, // Numpad *
                (minecraftClient -> WaypointHandler.listWaypoints())
        );

        registerPressedKeybind(
                "key.m0-dev-tools.open_waypoint_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_DIVIDE, // Numpad /
                (minecraftClient -> {
                    if (minecraftClient.currentScreen == null) {
                        minecraftClient.setScreen(WaypointScreen.create(null));
                    }
                })
        );

        registerPressedKeybind(
                "key.m0-dev-tools.toggle_waypoint_render",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_SUBTRACT, // Numpad -
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
