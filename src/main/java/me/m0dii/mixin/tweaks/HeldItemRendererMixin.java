package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void hideOffhandItem(AbstractClientPlayerEntity player,
                                 float tickProgress,
                                 float pitch,
                                 Hand hand,
                                 float swingProgress,
                                 ItemStack item,
                                 float equipProgress,
                                 MatrixStack matrices,
                                 OrderedRenderCommandQueue renderQueue,
                                 int light,
                                 CallbackInfo ci) {
        if (hand == Hand.OFF_HAND && TweaksModule.INSTANCE.hideOffhandItem()) {
            ci.cancel();
        }
    }
}
