package me.m0dii.modules.zoom;

import lombok.Getter;
import me.m0dii.modules.Module;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ZoomModule extends Module {

    public static final ZoomModule INSTANCE = new ZoomModule();

    @Getter
    public float zoomFov = 30f;

    @Getter
    public boolean held = false;

    protected ZoomModule() {
        super("zoom", "Zoom", false);
    }

    @Override
    public void register() {
        registerHeldKeybind(
                "key.m0-dev-tools.zoom",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                client -> held = true,
                client -> held = false
        );
    }

    public void onScroll(double horizontal, double vertical) {
        if (held) {
            if (vertical > 0) {
                zoomFov -= 5f;
                if (zoomFov < 5f) {
                    zoomFov = 5f;
                }
            } else if (vertical < 0) {
                zoomFov += 5f;
                if (zoomFov > 90f) {
                    zoomFov = 90f;
                }
            }

            getClient().player.sendMessage(Text.literal("Zoom FOV: " + (int) zoomFov), true);
        }
    }
}
