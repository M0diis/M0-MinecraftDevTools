package me.m0dii.modules.hungertweaks;

import me.m0dii.mixin.hungertweaks.HungerManagerAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class HungerTweaksFoodHelper {
    public static final FoodComponent EMPTY_FOOD_COMPONENT = new FoodComponent.Builder().build();
    public static final float REGEN_EXHAUSTION_INCREMENT = 6.0F;
    public static final float MAX_EXHAUSTION = 4.0F;

    private HungerTweaksFoodHelper() {
    }

    public static boolean isFood(ItemStack itemStack) {
        return itemStack.contains(DataComponentTypes.FOOD);
    }

    public static boolean canConsume(PlayerEntity player, FoodComponent foodComponent) {
        return player.canConsume(foodComponent.canAlwaysEat());
    }

    public static FoodComponent getDefaultFoodValues(ItemStack itemStack) {
        return itemStack.getOrDefault(DataComponentTypes.FOOD, EMPTY_FOOD_COMPONENT);
    }

    @Nullable
    public static QueriedFoodResult query(ItemStack itemStack, PlayerEntity player) {
        if (!isFood(itemStack)) {
            return null;
        }

        FoodComponent defaultFood = getDefaultFoodValues(itemStack);
        return new QueriedFoodResult(defaultFood, defaultFood, itemStack);
    }

    public static boolean isRotten(FoodComponent foodComponent) {
        return false;
    }

    public static float getExhaustion(HungerManager hungerManager) {
        return ((HungerManagerAccessor) hungerManager).m0dev$getExhaustion();
    }

    public static void setExhaustion(HungerManager hungerManager, float exhaustion) {
        ((HungerManagerAccessor) hungerManager).m0dev$setExhaustion(exhaustion);
    }

    public static float getEstimatedHealthIncrement(PlayerEntity player, FoodComponent foodComponent) {
        if (!player.canFoodHeal()) {
            return 0;
        }

        HungerManager stats = player.getHungerManager();
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

    public record QueriedFoodResult(FoodComponent defaultFoodComponent,
                                    FoodComponent modifiedFoodComponent,
                                    ItemStack itemStack) {
    }
}
