package me.m0dii.modules.quicktp;

import me.m0dii.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class QuickTeleportModule extends Module {

    public static final QuickTeleportModule INSTANCE = new QuickTeleportModule();

    public QuickTeleportModule() {
        super("quick_teleport", "Quick Teleportation", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(
                "key.m0-dev-tools.teleport_up",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_8, // Numpad 8
                (minecraftClient) -> teleport(0, 10, 0)
        );

        registerPressedKeybind(
                "key.m0-dev-tools.teleport_down",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_2, // Numpad 2
                (minecraftClient) -> teleport(0, -10, 0)
        );

        registerPressedKeybind(
                "key.m0-dev-tools.teleport_forward",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_5, // Numpad 5
                (minecraftClient) -> teleportForward(10)
        );

        registerPressedKeybind(
                "key.m0-dev-tools.teleport_left",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_4, // Numpad 4
                (minecraftClient) -> teleport(-10, 0, 0)
        );

        registerPressedKeybind(
                "key.m0-dev-tools.teleport_right",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_6, // Numpad 6
                (minecraftClient) -> teleport(10, 0, 0)
        );
    }

    private static void teleport(double x, double y, double z) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        var pos = client.player.getPos();
        client.player.setPosition(pos.x + x, pos.y + y, pos.z + z);

        client.player.sendMessage(
                Text.literal("§aTeleported §f" + String.format("%.1f, %.1f, %.1f", x, y, z)),
                true
        );
    }

    private static void teleportForward(double distance) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        var yaw = Math.toRadians(client.player.getYaw());
        double x = -Math.sin(yaw) * distance;
        double z = Math.cos(yaw) * distance;

        var pos = client.player.getPos();
        client.player.setPosition(pos.x + x, pos.y, pos.z + z);

        client.player.sendMessage(
                Text.literal("§aTeleported forward §f" + distance + " blocks"),
                true
        );
    }
}
