package me.m0dii.modules.nbthud;

import lombok.Getter;
import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class NBTInfoHudModule extends Module {

    public static final NBTInfoHudModule INSTANCE = new NBTInfoHudModule();

    @Getter
    private final NBTInfoHudRenderer renderer = new NBTInfoHudRenderer();

    private NBTInfoHudModule() {
        super("nbt_info_hud_overlay", "NBT Info HUD Overlay", false);
    }

    @Override
    public void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(M0DevToolsClient.MOD_ID, "nbt_info_hud"),
                renderer::onHudRender
        );

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
