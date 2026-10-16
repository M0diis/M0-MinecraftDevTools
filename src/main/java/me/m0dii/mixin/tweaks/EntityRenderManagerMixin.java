package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderManagerMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void disableEntityRendering(E entity,
                                                           Frustum frustum,
                                                           double x,
                                                           double y,
                                                           double z,
                                                           float partialTicks,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (TweaksModule.INSTANCE.disableEntityRendering()) {
            cir.setReturnValue(false);
        }
    }
}
