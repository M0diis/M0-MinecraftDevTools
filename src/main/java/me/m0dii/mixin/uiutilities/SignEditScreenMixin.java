package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public class SignEditScreenMixin extends Screen {

    protected SignEditScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private static final Minecraft mc = Minecraft.getInstance();

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isEnabled()) {
            addRenderableWidget(Button.builder(Component.nullToEmpty("Close without packet"), button -> {
                UiUtilitiesModule.INSTANCE.setShouldEditSign(false);
                mc.gui.setScreen(null);
            }).width(115).pos(5, 5).build());

            addRenderableWidget(Button.builder(Component.nullToEmpty("Disconnect"), button -> {
                if (mc.getConnection() != null) {
                    mc.getConnection().getConnection().disconnect(Component.nullToEmpty("Disconnecting (UI-UTILS)"));
                }
            }).width(115).pos(5, 35).build());
        }
    }
}
