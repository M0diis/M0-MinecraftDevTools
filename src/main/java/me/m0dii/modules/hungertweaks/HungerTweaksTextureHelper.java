package me.m0dii.modules.hungertweaks;

import me.m0dii.M0DevToolsClient;
import net.minecraft.util.Identifier;

public final class HungerTweaksTextureHelper {
    public static final Identifier MOD_ICONS = Identifier.of(M0DevToolsClient.MOD_ID, "textures/hungertweaks/icons.png");
    public static final Identifier HUNGER_OUTLINE_SPRITE = Identifier.of(M0DevToolsClient.MOD_ID, "hungertweaks/tooltip_hunger_outline");

    public static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
    public static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
    public static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
    public static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
    public static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
    public static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");

    public static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
    public static final Identifier HEART_HARDCORE_CONTAINER = Identifier.ofVanilla("hud/heart/container_hardcore");
    public static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    public static final Identifier HEART_HARDCORE_FULL = Identifier.ofVanilla("hud/heart/hardcore_full");
    public static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    public static final Identifier HEART_HARDCORE_HALF = Identifier.ofVanilla("hud/heart/hardcore_half");

    private HungerTweaksTextureHelper() {
    }

    public enum FoodType {
        EMPTY,
        HALF,
        FULL
    }

    public enum HeartType {
        CONTAINER,
        FULL,
        HALF
    }

    public static Identifier getFoodTexture(boolean rotten, FoodType type) {
        return switch (type) {
            case EMPTY -> rotten ? FOOD_EMPTY_HUNGER_TEXTURE : FOOD_EMPTY_TEXTURE;
            case HALF -> rotten ? FOOD_HALF_HUNGER_TEXTURE : FOOD_HALF_TEXTURE;
            case FULL -> rotten ? FOOD_FULL_HUNGER_TEXTURE : FOOD_FULL_TEXTURE;
        };
    }

    public static Identifier getHeartTexture(boolean hardcore, HeartType type) {
        return switch (type) {
            case CONTAINER -> hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
            case FULL -> hardcore ? HEART_HARDCORE_FULL : HEART_FULL;
            case HALF -> hardcore ? HEART_HARDCORE_HALF : HEART_HALF;
        };
    }
}
