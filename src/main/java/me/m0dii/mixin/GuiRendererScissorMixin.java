package me.m0dii.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Since 26.x the render backend rejects zero-sized scissors. Vanilla clips widget text to the widget bounds,
 * so any widget laid out fully outside the window (e.g. tall config screens on a small window) produced a
 * zero-height scissor and crashed the frame. Everything inside such a scissor is off-window anyway, so a
 * 1px minimum keeps the frame alive without changing what is visible.
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererScissorMixin {
    @WrapOperation(
            method = "enableScissor",
            at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/RenderPass;enableScissor(IIII)V")
    )
    private void m0devClampEmptyScissor(RenderPass renderPass, int x, int y, int width, int height, Operation<Void> original) {
        original.call(renderPass, x, y, Math.max(1, width), Math.max(1, height));
    }
}
