package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenMixin extends Screen {

    private MultiplayerScreenMixin() {
        super(null);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isEnabled()) {
            UiUtilitiesModule instance = UiUtilitiesModule.INSTANCE;

            this.addDrawableChild(ButtonWidget.builder(
                    Text.of("Bypass RP: " + (instance.isBypassResourcePack() ? "ON" : "OFF")),
                    button -> {
                        instance.setBypassResourcePack(!instance.isBypassResourcePack());
                        button.setMessage(Text.of("Bypass RP: " + (instance.isBypassResourcePack() ? "ON" : "OFF")));
                    }).width(90).position(5, 4).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.of("Force Deny: " + (instance.isResourcePackForceDeny() ? "ON" : "OFF")),
                    button -> {
                        instance.setResourcePackForceDeny(!instance.isResourcePackForceDeny());
                        button.setMessage(Text.of("Force Deny: " + (instance.isResourcePackForceDeny() ? "ON" : "OFF")));
                    }).width(90).position(this.width - 95, 4).build());
        }
    }
}
