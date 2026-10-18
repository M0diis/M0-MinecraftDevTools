package me.m0dii.mixin.glow;

import me.m0dii.modules.glow.GlowCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code /glow}: entities picked by type or group get the vanilla glowing outline, client side only. */
@Mixin(Minecraft.class)
public abstract class GlowMinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void m0dev$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (GlowCommand.colorFor(entity) != null) {
            cir.setReturnValue(true);
        }
    }
}
