package me.m0dii.modules.clickgui;

import lombok.Getter;
import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {

    public static final ClickGuiModule INSTANCE = new ClickGuiModule();

    @Getter
    private final ClickGuiRenderer renderer = new ClickGuiRenderer();

    private ClickGuiModule() {
        super("clickgui", "ClickGUI", true);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(renderer::onHudRender);

        registerPressedKeybind(
                "key.m0-dev-tools.clickgui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                client -> renderer.toggle()
        );
    }

}

