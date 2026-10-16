package me.m0dii.modules.macros.hud;

import me.m0dii.M0DevToolsClient;
import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class MacroHudModule extends Module {

    public static final MacroHudModule INSTANCE = new MacroHudModule();

    private MacroHudModule() {
        super("macro_hud", "Macro HUD", true);
    }

    @Override
    public void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(M0DevToolsClient.MOD_ID, "macro_hud"),
                this::onHudRender
        );
    }

    private void onHudRender(net.minecraft.client.gui.GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!isEnabled() || client.player == null || client.level == null) {
            return;
        }

        MacroHudRuntime.extractRenderState(context, MacroHudRuntime.isInteractiveContext());
    }
}

