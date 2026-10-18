package me.m0dii.mixin.glow;

import me.m0dii.modules.glow.GlowCommand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Outline colour for {@code /glow} targets (the outline uses the team colour). */
@Mixin(Entity.class)
public abstract class GlowEntityColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void m0dev$glowColor(CallbackInfoReturnable<Integer> cir) {
        Integer color = GlowCommand.colorFor((Entity) (Object) this);
        if (color != null) {
            cir.setReturnValue(color);
        }
    }
}
