package me.m0dii.modules.freecam;

import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FreecamModule extends Module {

    public static final FreecamModule INSTANCE = new FreecamModule();

    protected FreecamModule() {
        super("freecam", "Freecam", false);
    }

    @Override
    public void register() {
        registerPressedKeybind("key.m0-dev-tools.freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                client -> toggleEnabled());
    }

    @Override
    public void onEnable() {
        CameraEntity.setCameraState(true);
    }

    @Override
    public void onDisable() {
        CameraEntity.setCameraState(false);
    }
}
