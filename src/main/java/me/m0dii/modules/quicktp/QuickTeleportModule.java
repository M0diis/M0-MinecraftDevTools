package me.m0dii.modules.quicktp;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class QuickTeleportModule extends Module {

    public static final QuickTeleportModule INSTANCE = new QuickTeleportModule();

    public QuickTeleportModule() {
        super("quick_teleport", "Quick Teleportation", true);
    }

    @Override
    public void register() {
        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_UP.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.QUICK_TP_UP.defaultKey(),
                (minecraftClient) -> teleport(0, 10, 0)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_DOWN.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.QUICK_TP_DOWN.defaultKey(),
                (minecraftClient) -> teleport(0, -10, 0)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_FORWARD.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.QUICK_TP_FORWARD.defaultKey(),
                (minecraftClient) -> teleportForward(10)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_LEFT.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.QUICK_TP_LEFT.defaultKey(),
                (minecraftClient) -> teleport(-10, 0, 0)
        );

        registerPressedKeybind(
                KeybindCatalog.QUICK_TP_RIGHT.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.QUICK_TP_RIGHT.defaultKey(),
                (minecraftClient) -> teleport(10, 0, 0)
        );
    }

    private static void teleport(double x, double y, double z) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        var pos = client.player.position();
        client.player.setPos(pos.x() + x, pos.y() + y, pos.z() + z);

        client.player.sendOverlayMessage(
                Component.literal("§aTeleported §f" + String.format("%.1f, %.1f, %.1f", x, y, z)));
    }

    private static void teleportForward(double distance) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        var yaw = Math.toRadians(client.player.getYRot());
        double x = -Math.sin(yaw) * distance;
        double z = Math.cos(yaw) * distance;

        var pos = client.player.position();
        client.player.setPos(pos.x() + x, pos.y(), pos.z() + z);

        client.player.sendOverlayMessage(
                Component.literal("§aTeleported forward §f" + distance + " blocks"));
    }
}
