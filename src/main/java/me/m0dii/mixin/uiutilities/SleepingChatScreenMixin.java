package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InBedChatScreen.class)
public class SleepingChatScreenMixin extends Screen {

    protected SleepingChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isEnabled()) {
            addRenderableWidget(Button.builder(Component.nullToEmpty("Client wake up"), button -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.stopSleeping();
                    this.minecraft.gui.setScreen(null);
                }
            }).width(115).pos(5, 5).build());
        }
    }
}
