package me.m0dii.modules.macros.hud;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public class MacroHudOverlayModule extends Module {

    public static final MacroHudOverlayModule INSTANCE = new MacroHudOverlayModule();

    private MacroHudOverlayModule() {
        super("macro_hud_overlay", "Macro HUD Overlay", true);
    }

    @Override
    public void register() {
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(net.minecraft.client.gui.DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isEnabled() || client.player == null || client.world == null) {
            return;
        }

        MacroHudRuntime.render(context, MacroHudRuntime.isInteractiveContext());
    }
}

