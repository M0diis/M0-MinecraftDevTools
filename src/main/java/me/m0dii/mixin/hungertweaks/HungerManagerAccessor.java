package me.m0dii.mixin.hungertweaks;

import net.minecraft.entity.player.HungerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HungerManager.class)
public interface HungerManagerAccessor {
    @Accessor("exhaustion")
    float m0dev$getExhaustion();

    @Accessor("exhaustion")
    void m0dev$setExhaustion(float exhaustion);
}
