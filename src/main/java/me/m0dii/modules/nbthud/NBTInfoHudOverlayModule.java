package me.m0dii.modules.nbthud;

import lombok.Getter;
import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class NBTInfoHudOverlayModule extends Module {

    public static final NBTInfoHudOverlayModule INSTANCE = new NBTInfoHudOverlayModule();

    @Getter
    private final NBTInfoHudRenderer renderer = new NBTInfoHudRenderer();

    private NBTInfoHudOverlayModule() {
        super("nbt_info_hud_overlay", "NBT Info HUD Overlay", false);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(renderer::onHudRender);

        registerPressedKeybind("key.m0-dev-tools.toggle_block_inspector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F10,
                client -> {
                    toggleEnabled();
                    renderer.setEnabled(isEnabled());
                });
    }

    @Override
    public void onEnable() {
        renderer.setEnabled(true);
    }

    @Override
    public void onDisable() {
        renderer.setEnabled(false);
    }


}
