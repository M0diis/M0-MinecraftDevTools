package me.m0dii.mixin.theme;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import me.m0dii.gui.theme.Theme;
import me.m0dii.gui.theme.ThemeDraw;
import me.m0dii.gui.theme.ThemedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EditBox.class)
public abstract class ThemedEditBoxMixin {
    @WrapOperation(
            method = "extractWidgetRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void m0devThemedBackground(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                       int x, int y, int w, int h, Operation<Void> original) {
        if (!ThemedScreen.isActive()) {
            original.call(graphics, pipeline, sprite, x, y, w, h);
            return;
        }
        EditBox self = (EditBox) (Object) this;
        ThemeDraw.box(graphics, x, y, w, h, Theme.INPUT);
        int border = !self.isActive() ? Theme.DIVIDER : self.isFocused() ? Theme.ACCENT : self.isHovered() ? Theme.BORDER_STRONG : Theme.BORDER;
        ThemeDraw.outline(graphics, x, y, w, h, border);
    }
}
