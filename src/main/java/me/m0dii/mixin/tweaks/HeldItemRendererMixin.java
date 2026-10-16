package me.m0dii.mixin.tweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class HeldItemRendererMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hideOffhandItem(PlayerRenderState player,
                                 FirstPersonHandsAndItemsRenderState state,
                                 float tickProgress,
                                 float pitch,
                                 InteractionHand hand,
                                 float swingProgress,
                                 ItemStack item,
                                 float equipProgress,
                                 PoseStack matrices,
                                 SubmitNodeCollector renderQueue,
                                 int light,
                                 CallbackInfo ci) {
        if (hand == InteractionHand.OFF_HAND && TweaksModule.INSTANCE.hideOffhandItem()) {
            ci.cancel();
        }
    }
}
