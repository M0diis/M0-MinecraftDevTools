package me.m0dii.modules.waypoints;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WaypointModule extends Module {
    public static final WaypointModule INSTANCE = new WaypointModule();

    protected WaypointModule() {
        super("waypoint_module", "Waypoint Module", false);
    }

    @Override
    public void register() {
        WaypointCommands.register();
        WaypointHandler.register();

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
    }
}
