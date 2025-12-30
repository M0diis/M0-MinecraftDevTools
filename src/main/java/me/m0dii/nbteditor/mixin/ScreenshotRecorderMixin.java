package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.TextUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(ScreenshotRecorder.class)
public class ScreenshotRecorderMixin {
    @ModifyVariable(method = "saveScreenshotInner", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    @Group(name = "saveScreenshotInner", min = 1)
    private static Consumer<Text> saveScreenshotInner(Consumer<Text> receiver) {
        if (!ConfigScreen.isScreenshotOptions()) {
            return receiver;
        }
        return msg -> receiver.accept(TextUtil.attachFileTextOptions(TextInst.copy(msg), MixinLink.screenshotTarget));
    }
}
