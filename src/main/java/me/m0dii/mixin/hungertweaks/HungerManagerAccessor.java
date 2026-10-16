package me.m0dii.mixin.hungertweaks;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface HungerManagerAccessor {
    @Accessor("exhaustionLevel")
    float m0dev$getExhaustion();

    @Accessor("exhaustionLevel")
    void m0dev$setExhaustion(float exhaustion);
}
