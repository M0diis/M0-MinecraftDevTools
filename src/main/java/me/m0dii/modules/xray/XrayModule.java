package me.m0dii.modules.xray;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class XrayModule extends Module {

    public static final XrayModule INSTANCE = new XrayModule();

    public XrayModule() {
        super("xray", "Xray", false);
    }

    @Override
    public void register() {
        registerPressedKeybind("key.m0-dev-tools.xray_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                client -> toggleEnabled());

        registerPressedKeybind("key.m0-dev-tools.xray_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                client -> client.setScreen(XrayConfigScreen.create(client.currentScreen)));

    }
}

