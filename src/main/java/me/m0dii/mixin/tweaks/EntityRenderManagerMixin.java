package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderManager.class)
public class EntityRenderManagerMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void disableEntityRendering(E entity,
                                                           Frustum frustum,
                                                           double x,
                                                           double y,
                                                           double z,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (TweaksModule.INSTANCE.disableEntityRendering()) {
            cir.setReturnValue(false);
        }
    }
}
