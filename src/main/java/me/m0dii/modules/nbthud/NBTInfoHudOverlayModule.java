package me.m0dii.modules.nbthud;

import lombok.Getter;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.util.InputUtil;

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

        registerPressedKeybind(KeybindCatalog.BLOCK_INSPECTOR_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.BLOCK_INSPECTOR_TOGGLE.defaultKey(),
                client -> toggleEnabled());
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
