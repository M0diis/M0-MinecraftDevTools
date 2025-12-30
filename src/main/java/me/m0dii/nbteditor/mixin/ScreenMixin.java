package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.screens.ImportScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "onFilesDropped", at = @At("HEAD"))
    private void onFilesDropped(List<Path> paths, CallbackInfo info) {
        Screen source = (Screen) (Object) this;
        if (source instanceof HandledScreen || source instanceof GameMenuScreen) {
            ImportScreen.importFiles(paths, Optional.empty());
        }
    }

    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
    private void handleTextClick(Style style, CallbackInfoReturnable<Boolean> info) {
        if (style != null && !Screen.hasShiftDown() && style.getClickEvent() != null &&
                style.getClickEvent().getAction() == ClickEvent.Action.OPEN_FILE &&
                MixinLink.tryRunClickEvent(style.getClickEvent().getValue())) {
            info.setReturnValue(true);
        }
    }

    // See toggled.ScreenMixin#renderTooltipFromComponents, toggled.DrawContextMixin#drawTooltip
    @Inject(method = "method_32633(Lnet/minecraft/class_4587;Ljava/util/List;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_4587;method_22903()V", shift = At.Shift.AFTER), remap = false, require = 0)
    @SuppressWarnings("target")
    private void renderTooltipFromComponents(MatrixStack matrices, List<TooltipComponent> tooltip, int x, int y, CallbackInfo info) {
        if (!ConfigScreen.isTooltipOverflowFix()) {
            return;
        }

        int[] size = MixinLink.getTooltipSize(tooltip);
        int width = size[0];
        int height = size[1];
        int screenWidth = MiscUtil.client.currentScreen.width;
        int screenHeight = MiscUtil.client.currentScreen.height;

        x += 12;
        y -= 12;
        if (x + width > screenWidth) {
            x -= 28 + width;
        }
        if (y + height + 6 > screenHeight) {
            y = screenHeight - height - 6;
        }

        MixinLink.renderTooltipFromComponents(matrices, x, y, width, height, screenWidth, screenHeight);
    }
}
