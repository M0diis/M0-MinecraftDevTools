package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookScreen.class)
public class BookScreenMixin extends Screen {

    protected BookScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isEnabled()) {
            UiUtilitiesModule.INSTANCE.createUtilityButtons(mc, this);
            TextFieldWidget addressField = UiUtilitiesModule.createInputField(this.textRenderer, mc);
            this.addDrawableChild(addressField);
        }
    }
}
