package me.m0dii.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobEntityAccessor {
    @Accessor("goalSelector")
    GoalSelector m0dev$getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector m0dev$getTargetSelector();
}
