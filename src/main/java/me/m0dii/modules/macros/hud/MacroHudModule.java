package me.m0dii.modules.macros.hud;

import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public class MacroHudModule extends Module {

    public static final MacroHudModule INSTANCE = new MacroHudModule();

    private MacroHudModule() {
        super("macro_hud", "Macro HUD", true);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(M0DevToolsClient.MOD_ID, "macro_hud"),
                this::onHudRender
        );
    }

    private void onHudRender(net.minecraft.client.gui.DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled() || client.player == null || client.world == null) {
            return;
        }

        MacroHudRuntime.render(context, MacroHudRuntime.isInteractiveContext());
    }
}

