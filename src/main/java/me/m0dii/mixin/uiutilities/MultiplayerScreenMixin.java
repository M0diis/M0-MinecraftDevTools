package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {

    private MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isEnabled()) {
            UiUtilitiesModule instance = UiUtilitiesModule.INSTANCE;

            this.addRenderableWidget(Button.builder(
                    Component.nullToEmpty("Bypass RP: " + (instance.isBypassResourcePack() ? "ON" : "OFF")),
                    button -> {
                        instance.setBypassResourcePack(!instance.isBypassResourcePack());
                        button.setMessage(net.minecraft.network.chat.Component.literal("Bypass RP: " + (instance.isBypassResourcePack() ? "ON" : "OFF")));
                    }).width(90).pos(5, 4).build());

            this.addRenderableWidget(Button.builder(
                    Component.nullToEmpty("Force Deny: " + (instance.isResourcePackForceDeny() ? "ON" : "OFF")),
                    button -> {
                        instance.setResourcePackForceDeny(!instance.isResourcePackForceDeny());
                        button.setMessage(net.minecraft.network.chat.Component.literal("Force Deny: " + (instance.isResourcePackForceDeny() ? "ON" : "OFF")));
                    }).width(90).pos(this.width - 95, 4).build());
        }
    }
}
