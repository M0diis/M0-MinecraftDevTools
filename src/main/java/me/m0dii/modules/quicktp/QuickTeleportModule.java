package me.m0dii.modules.quicktp;

import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class QuickTeleportModule extends Module {

    public static final QuickTeleportModule INSTANCE = new QuickTeleportModule();

    public QuickTeleportModule() {
        super("quick_teleport", "Quick Teleportation", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_UP.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.QUICK_TP_UP.defaultKey(),
                (minecraftClient) -> teleport(0, 10, 0)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_DOWN.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.QUICK_TP_DOWN.defaultKey(),
                (minecraftClient) -> teleport(0, -10, 0)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_FORWARD.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.QUICK_TP_FORWARD.defaultKey(),
                (minecraftClient) -> teleportForward(10)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_LEFT.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.QUICK_TP_LEFT.defaultKey(),
                (minecraftClient) -> teleport(-10, 0, 0)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_RIGHT.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.QUICK_TP_RIGHT.defaultKey(),
                (minecraftClient) -> teleport(10, 0, 0)
        );
    }

    private static void teleport(double x, double y, double z) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        var pos = client.player.getEntityPos();
        client.player.setPosition(pos.getX() + x, pos.getY() + y, pos.getZ() + z);

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

        var pos = client.player.getEntityPos();
        client.player.setPosition(pos.getX() + x, pos.getY(), pos.getZ() + z);

        client.player.sendMessage(
                Text.literal("§aTeleported forward §f" + distance + " blocks"),
                true
        );
    }
}
