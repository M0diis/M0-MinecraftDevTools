package me.m0dii.modules.fullbright;

import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FullbrightModule extends Module {

    public static final FullbrightModule INSTANCE = new FullbrightModule();

    @Getter
    private double gammaValue = ModConfig.fullbrightGamma;

    protected FullbrightModule() {
        super("fullbright", "Fullbright", false);
    }

    @Override
    public void register() {
        registerPressedKeybind(
                "key.m0-dev-tools.fullbright",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                client -> toggleEnabled()
        );
    }
}
