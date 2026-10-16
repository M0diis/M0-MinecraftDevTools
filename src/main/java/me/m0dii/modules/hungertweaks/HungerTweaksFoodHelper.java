package me.m0dii.modules.hungertweaks;

import me.m0dii.mixin.hungertweaks.HungerManagerAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class HungerTweaksFoodHelper {
    public static final FoodProperties EMPTY_FOOD_COMPONENT = new FoodProperties.Builder().build();
    public static final float REGEN_EXHAUSTION_INCREMENT = 6.0F;
    public static final float MAX_EXHAUSTION = 4.0F;

    private HungerTweaksFoodHelper() {
    }

    public static boolean isFood(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD);
    }

    public static boolean canConsume(Player player, FoodProperties foodComponent) {
        return player.canEat(foodComponent.canAlwaysEat());
    }

    public static FoodProperties getDefaultFoodValues(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponents.FOOD, EMPTY_FOOD_COMPONENT);
    }

    @Nullable
    public static QueriedFoodResult query(ItemStack itemStack, Player player) {
        if (!isFood(itemStack)) {
            return null;
        }

        FoodProperties defaultFood = getDefaultFoodValues(itemStack);
        return new QueriedFoodResult(defaultFood, defaultFood, itemStack);
    }

    public static boolean isRotten(FoodProperties foodComponent) {
        return false;
    }

    public static float getExhaustion(FoodData hungerManager) {
        return ((HungerManagerAccessor) hungerManager).m0dev$getExhaustion();
    }

    public static void setExhaustion(FoodData hungerManager, float exhaustion) {
        ((HungerManagerAccessor) hungerManager).m0dev$setExhaustion(exhaustion);
    }

    public static float getEstimatedHealthIncrement(Player player, FoodProperties foodComponent) {
        if (!player.isHurt()) {
            return 0;
        }

        FoodData stats = player.getFoodData();
        int foodLevel = Math.min(stats.getFoodLevel() + foodComponent.nutrition(), 20);
        if (foodLevel < 18) {
            return 0;
        }

        float saturationLevel = Math.min(stats.getSaturationLevel() + foodComponent.saturation(), foodLevel);
        return getEstimatedHealthIncrement(foodLevel, saturationLevel, getExhaustion(stats));
    }

    public static float getEstimatedHealthIncrement(int foodLevel, float saturationLevel, float exhaustionLevel) {
        float health = 0;

        if (!Float.isFinite(exhaustionLevel) || !Float.isFinite(saturationLevel)) {
            return 0;
        }

        while (foodLevel >= 18) {
            while (exhaustionLevel > MAX_EXHAUSTION) {
                exhaustionLevel -= MAX_EXHAUSTION;
                if (saturationLevel > 0) {
                    saturationLevel = Math.max(saturationLevel - 1, 0);
                } else {
                    foodLevel -= 1;
                }
            }

            if (foodLevel >= 20 && Float.compare(saturationLevel, Float.MIN_NORMAL) > 0) {
                float limitedSaturationLevel = Math.min(saturationLevel, REGEN_EXHAUSTION_INCREMENT);
                float exhaustionUntilAboveMax = Math.nextUp(MAX_EXHAUSTION) - exhaustionLevel;
                int numIterationsUntilAboveMax = Math.max(1, (int) Math.ceil(exhaustionUntilAboveMax / limitedSaturationLevel));
                health += (limitedSaturationLevel / REGEN_EXHAUSTION_INCREMENT) * numIterationsUntilAboveMax;
                exhaustionLevel += limitedSaturationLevel * numIterationsUntilAboveMax;
            } else if (foodLevel >= 18) {
                health += 1;
                exhaustionLevel += REGEN_EXHAUSTION_INCREMENT;
            }
        }

        return health;
    }

    public record QueriedFoodResult(FoodProperties defaultFoodComponent,
                                    FoodProperties modifiedFoodComponent,
                                    ItemStack itemStack) {
    }
}
