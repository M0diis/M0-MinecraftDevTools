package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.MVMatrix4f;
import me.m0dii.nbteditor.multiversion.MVMisc;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Shadow
    public abstract MatrixStack getMatrices();

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER))
    @Group(name = "drawTooltip", min = 1)
    private void drawTooltip(TextRenderer textRenderer, List<TooltipComponent> tooltip, int x, int y, TooltipPositioner positioner, Identifier texture, CallbackInfo info) {
        draw(tooltip, x, y, positioner);
    }

    private void draw(List<TooltipComponent> tooltip, int x, int y, TooltipPositioner positioner) {
        if (!ConfigScreen.isTooltipOverflowFix()) {
            return;
        }

        int[] size = MixinLink.getTooltipSize(tooltip);
        Vector2ic pos = MVMisc.getPosition(positioner, MiscUtil.client.currentScreen, x, y, size[0], size[1]);
        int screenWidth = MiscUtil.client.getWindow().getScaledWidth();
        int screenHeight = MiscUtil.client.getWindow().getScaledHeight();

        MixinLink.renderTooltipFromComponents(getMatrices(), pos.x(), pos.y(), size[0], size[1], screenWidth, screenHeight);
    }

    @ModifyVariable(method = "scissorContains", at = @At("HEAD"), ordinal = 0, require = 0)
    private int scissorContainsX(int x) {
        float[] translation = MVMatrix4f.getTranslation(getMatrices());
        return x + (int) translation[0];
    }

    @ModifyVariable(method = "scissorContains", at = @At("HEAD"), ordinal = 1, require = 0)
    private int scissorContainsY(int y) {
        float[] translation = MVMatrix4f.getTranslation(getMatrices());
        return y + (int) translation[1];
    }

}
