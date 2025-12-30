package me.m0dii.modules.xray;

import me.m0dii.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class XrayModule extends Module {

    public static final XrayModule INSTANCE = new XrayModule();

    public XrayModule() {
        super("xray", "Xray", false);
    }

    @Override
    public void register() {
        registerPressedKeybind("key.m0-dev-tools.xray_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, (MinecraftClient client) -> {
            XrayManager.toggleXray();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Xray mode: " + (XrayManager.isXrayEnabled() ? "ON" : "OFF")), true);
            }
            setEnabled(XrayManager.isXrayEnabled());
        });

        registerPressedKeybind("key.m0-dev-tools.xray_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, client -> {
            Screen screen = XrayConfigScreen.create(client.currentScreen);
            client.setScreen(screen);
        });

    }

    @Override
    protected void onEnable() {
        if (!XrayManager.isXrayEnabled()) {
            XrayManager.toggleXray();
        }
    }

    @Override
    protected void onDisable() {
        if (XrayManager.isXrayEnabled()) {
            XrayManager.toggleXray();
        }
    }
}

