package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Util.class)
public class UtilMixin {

    @Inject(method = "logErrorOrPause(Ljava/lang/String;Ljava/lang/Throwable;)V", at = @At("HEAD"), require = 0)
    private static void logErrorOrPause(String message, Throwable throwable, CallbackInfo info) {
        if (MixinLink.hiddenExceptionHandlers.contains(Thread.currentThread())) {
            throw new MixinLink.HiddenException(message, throwable);
        }
    }

    @Inject(method = "logErrorOrPause(Ljava/lang/String;)V", at = @At("HEAD"))
    private static void logErrorOrPause(String message, CallbackInfo info) {
        if (MixinLink.hiddenExceptionHandlers.contains(Thread.currentThread())) {
            throw new MixinLink.HiddenException(message, new RuntimeException(message));
        }
    }
}
