package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(NativeImage.class)
public class NativeImageMixin {
    @Inject(method = "writeTo(Ljava/io/File;)V", at = @At("HEAD"))
    private void writeTo(File file, CallbackInfo info) {
        MixinLink.screenshotTarget = file;
    }
}
