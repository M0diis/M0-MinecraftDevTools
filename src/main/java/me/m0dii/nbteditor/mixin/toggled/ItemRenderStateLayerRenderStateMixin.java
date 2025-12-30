package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.nbteditor.misc.MixinLink;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemRenderState.LayerRenderState.class)
public class ItemRenderStateLayerRenderStateMixin {

    @Shadow
    private Glint glint;

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/model/special/SpecialModelRenderer;render(Ljava/lang/Object;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIZ)V"))
    private VertexConsumerProvider render(VertexConsumerProvider provider) {
        ItemStack item = MixinLink.ITEM_BEING_RENDERED.remove(Thread.currentThread());
        if (item == null) {
            return provider;
        }

        return layer -> ItemRenderer.getItemGlintConsumer(provider, layer, true, glint != Glint.NONE);
    }

}
