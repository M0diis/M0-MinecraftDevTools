package me.m0dii.nbteditor.mixin;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void setOverlay(Overlay overlay, CallbackInfo info) {
        if (((MinecraftClient) (Object) this).getOverlay() instanceof SplashOverlay && overlay == null && !MixinLink.CLIENT_LOADED) {
            MixinLink.CLIENT_LOADED = true;
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void setScreen(Screen screen, CallbackInfo info) {
        if (screen == null) {
            M0DevToolsClient.CURSOR_MANAGER.onNoScreenSet();
        } else if (screen instanceof HandledScreen<?> handledScreen) {
            M0DevToolsClient.CURSOR_MANAGER.onHandledScreenSet(handledScreen);
        }
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantThreadExecutor;<init>(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void init(RunArgs args, CallbackInfo info) {
        MixinLink.MAIN_THREAD = Thread.currentThread();
    }

}
