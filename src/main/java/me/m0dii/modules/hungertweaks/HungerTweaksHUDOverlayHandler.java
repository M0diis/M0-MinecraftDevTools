package me.m0dii.modules.hungertweaks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.Vector;

public class HungerTweaksHUDOverlayHandler {
    public static HungerTweaksHUDOverlayHandler INSTANCE;

    private float unclampedFlashAlpha = 0f;
    private float flashAlpha = 0f;
    private byte alphaDir = 1;

    public final OffsetsCache barOffsets = new OffsetsCache();
    public final HeldFoodCache heldFood = new HeldFoodCache();

    public static void init() {
        INSTANCE = new HungerTweaksHUDOverlayHandler();
    }

    public void onPreRenderFood(DrawContext context, PlayerEntity player, int top, int right) {
        if (player == null || !HungerTweaksModule.INSTANCE.showFoodExhaustionHudUnderlay()) {
            return;
        }

        drawExhaustionOverlay(context, HungerTweaksFoodHelper.getExhaustion(player.getHungerManager()), right, top);
    }

    public void onRenderFood(DrawContext context, PlayerEntity player, int top, int right) {
        if (player == null || !shouldRenderAnyOverlays()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        HungerManager stats = player.getHungerManager();

        if (HungerTweaksModule.INSTANCE.showSaturationHudOverlay()) {
            drawSaturationOverlay(context, 0, stats.getSaturationLevel(), right, top, client.inGameHud.getTicks());
        }

        HungerTweaksFoodHelper.QueriedFoodResult result = heldFood.result(client.inGameHud.getTicks(), player);
        if (result == null) {
            resetFlash();
            return;
        }
        if (!HungerTweaksModule.INSTANCE.showFoodValuesHudOverlay()) {
            return;
        }

        int foodHunger = result.modifiedFoodComponent().nutrition();
        float foodSaturationIncrement = result.modifiedFoodComponent().saturation();
        drawHungerOverlay(context, foodHunger, stats.getFoodLevel(), right, top, flashAlpha,
                HungerTweaksFoodHelper.isRotten(result.modifiedFoodComponent()), client.inGameHud.getTicks());

        if (HungerTweaksModule.INSTANCE.showSaturationHudOverlay()) {
            int newFoodValue = stats.getFoodLevel() + foodHunger;
            float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;
            float saturationGained = newSaturationValue > newFoodValue
                    ? newFoodValue - stats.getSaturationLevel()
                    : foodSaturationIncrement;
            drawSaturationOverlay(context, saturationGained, stats.getSaturationLevel(), right, top, client.inGameHud.getTicks());
        }
    }

    public void onRenderHealth(DrawContext context, PlayerEntity player, int left, int top, int lines,
                               int regeneratingHeartIndex, float maxHealth, int lastHealth, int health,
                               int absorption, boolean blinking) {
        if (player == null || !shouldRenderAnyOverlays()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        HungerTweaksFoodHelper.QueriedFoodResult result = heldFood.result(client.inGameHud.getTicks(), player);
        if (result == null) {
            resetFlash();
            return;
        }
        if (!shouldShowEstimatedHealth(player, client.inGameHud.getTicks())) {
            return;
        }

        float foodHealthIncrement = HungerTweaksFoodHelper.getEstimatedHealthIncrement(player, result.modifiedFoodComponent());
        float currentHealth = player.getHealth();
        float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());
        if (currentHealth < modifiedHealth) {
            drawHealthOverlay(context, currentHealth, modifiedHealth, left, top, flashAlpha, client.inGameHud.getTicks());
        }
    }

    public void drawSaturationOverlay(DrawContext context, float saturationGained, float saturationLevel, int right, int top, int guiTicks) {
        if (saturationLevel + saturationGained < 0) {
            return;
        }

        float modifiedSaturation = Math.clamp(saturationLevel + saturationGained, 0, 20);
        int startSaturationBar = saturationGained != 0 ? (int) Math.max(saturationLevel / 2.0F, 0) : 0;
        int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);
        int iconSize = 9;

        var foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, MinecraftClient.getInstance().player);
        for (int i = startSaturationBar; i < endSaturationBar; ++i) {
            IntPoint offset = i < foodBarOffsets.size() ? foodBarOffsets.get(i) : new IntPoint();
            int x = right + offset.x;
            int y = top + offset.y;
            float effectiveSaturationOfBar = (modifiedSaturation / 2.0F) - i;
            int u = effectiveSaturationOfBar >= 1 ? 27 : effectiveSaturationOfBar > .5 ? 18 : effectiveSaturationOfBar > .25 ? 9 : 0;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.MOD_ICONS, x, y, (float) u, 0f, iconSize, iconSize, 256, 256);
        }
    }

    public void drawHungerOverlay(DrawContext context, int hungerRestored, int foodLevel, int right, int top,
                                  float alpha, boolean rotten, int guiTicks) {
        if (hungerRestored <= 0) {
            return;
        }

        int modifiedFood = Math.clamp(foodLevel + hungerRestored, 0, 20);
        int startFoodBars = Math.max(0, foodLevel / 2);
        int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);
        int iconSize = 9;

        var foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, MinecraftClient.getInstance().player);
        for (int i = startFoodBars; i < endFoodBars; ++i) {
            IntPoint offset = i < foodBarOffsets.size() ? foodBarOffsets.get(i) : new IntPoint();
            int x = right + offset.x;
            int y = top + offset.y;
            Identifier backgroundSprite = HungerTweaksTextureHelper.getFoodTexture(rotten, HungerTweaksTextureHelper.FoodType.EMPTY);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, alpha * 0.25f);

            boolean half = i * 2 + 1 == modifiedFood;
            Identifier iconSprite = HungerTweaksTextureHelper.getFoodTexture(rotten, half ? HungerTweaksTextureHelper.FoodType.HALF : HungerTweaksTextureHelper.FoodType.FULL);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, alpha);
        }
    }

    public void drawHealthOverlay(DrawContext context, float health, float modifiedHealth, int right, int top, float alpha, int guiTicks) {
        if (modifiedHealth <= health) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
        boolean hardcore = client.world != null && client.world.getLevelProperties().isHardcore();
        int startHealthBars = (int) Math.max(0, Math.ceil(health) / 2.0F);
        int endHealthBars = (int) Math.max(0, Math.ceil(modifiedHealth / 2.0F));
        int iconSize = 9;

        var healthBarOffsets = barOffsets.healthBarOffsets(guiTicks, client.player);
        for (int i = startHealthBars; i < endHealthBars; ++i) {
            IntPoint offset = i < healthBarOffsets.size() ? healthBarOffsets.get(i) : new IntPoint();
            int x = right + offset.x;
            int y = top + offset.y;
            Identifier backgroundSprite = HungerTweaksTextureHelper.getHeartTexture(hardcore, HungerTweaksTextureHelper.HeartType.CONTAINER);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, alpha * 0.25f);

            boolean half = i * 2 + 1 == fixedModifiedHealth;
            Identifier iconSprite = HungerTweaksTextureHelper.getHeartTexture(hardcore, half ? HungerTweaksTextureHelper.HeartType.HALF : HungerTweaksTextureHelper.HeartType.FULL);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, alpha);
        }
    }

    public void drawExhaustionOverlay(DrawContext context, float exhaustion, int right, int top) {
        float ratio = Math.clamp(exhaustion / HungerTweaksFoodHelper.MAX_EXHAUSTION, 0, 1);
        int width = (int) (ratio * 81);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.MOD_ICONS, right - width, top, (float) (81 - width), 18f, width, 9, 256, 256);
    }

    public void onClientTick() {
        unclampedFlashAlpha += alphaDir * 0.125F;
        if (unclampedFlashAlpha >= 1.5F) {
            alphaDir = -1;
        } else if (unclampedFlashAlpha <= -0.5F) {
            alphaDir = 1;
        }
        flashAlpha = Math.clamp(unclampedFlashAlpha, 0F, 1F) * HungerTweaksModule.INSTANCE.getMaxHudOverlayFlashAlpha();
    }

    public void resetFlash() {
        unclampedFlashAlpha = 0;
        flashAlpha = 0;
        alphaDir = 1;
    }

    private boolean shouldRenderAnyOverlays() {
        return HungerTweaksModule.INSTANCE.showFoodValuesHudOverlay()
                || HungerTweaksModule.INSTANCE.showSaturationHudOverlay()
                || HungerTweaksModule.INSTANCE.showFoodHealthHudOverlay();
    }

    private boolean shouldShowEstimatedHealth(PlayerEntity player, int guiTicks) {
        if (!HungerTweaksModule.INSTANCE.showFoodHealthHudOverlay()) {
            return false;
        }
        if (barOffsets.healthBarOffsets(guiTicks, player).isEmpty()) {
            return false;
        }
        HungerManager stats = player.getHungerManager();
        if (player.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL || stats.getFoodLevel() >= 18) {
            return false;
        }
        return !player.hasStatusEffect(StatusEffects.POISON)
                && !player.hasStatusEffect(StatusEffects.WITHER)
                && !player.hasStatusEffect(StatusEffects.REGENERATION);
    }

    private static class OffsetsCache {
        protected final Vector<IntPoint> foodBarOffsets = new Vector<>();
        protected final Vector<IntPoint> healthBarOffsets = new Vector<>();
        public int lastGuiTick = 0;
        protected final Random random = new Random();

        protected void generate(int guiTicks, PlayerEntity player) {
            final int preferHealthBars = 10;
            final int preferFoodBars = 10;
            float maxHealth = player.getMaxHealth();
            float absorptionHealth = (float) Math.ceil(player.getAbsorptionAmount());

            int healthBars = (int) Math.ceil((maxHealth + absorptionHealth) / 2.0F);
            if (healthBars < 0 || healthBars > 1000) {
                healthBars = 0;
            }

            int healthRows = (int) Math.ceil((float) healthBars / preferHealthBars);
            int healthRowHeight = Math.max(10 - (healthRows - 2), 3);
            boolean animatedHealth = false;
            boolean animatedFood = false;
            if (HungerTweaksModule.INSTANCE.showVanillaAnimationsOverlay()) {
                HungerManager hungerManager = player.getHungerManager();
                float saturationLevel = hungerManager.getSaturationLevel();
                int foodLevel = hungerManager.getFoodLevel();
                animatedFood = saturationLevel <= 0.0F && guiTicks % (foodLevel * 3 + 1) == 0;
                animatedHealth = Math.ceil(player.getHealth()) <= 4;
            }

            random.setSeed((long) guiTicks * 312871L);
            if (healthBarOffsets.size() != healthBars) {
                healthBarOffsets.setSize(healthBars);
            }
            if (foodBarOffsets.size() != preferFoodBars) {
                foodBarOffsets.setSize(preferFoodBars);
            }

            for (int i = healthBars - 1; i >= 0; --i) {
                int row = (int) Math.ceil((float) (i + 1) / preferHealthBars) - 1;
                int x = i % preferHealthBars * 8;
                int y = -(row * healthRowHeight);
                if (animatedHealth) {
                    y += random.nextInt(2);
                }

                IntPoint point = healthBarOffsets.get(i);
                if (point == null) {
                    point = new IntPoint();
                    healthBarOffsets.set(i, point);
                }
                point.x = x;
                point.y = y;
            }

            for (int i = 0; i < preferFoodBars; ++i) {
                int x = -(i * 8) - 9;
                int y = 0;
                if (animatedFood) {
                    y += random.nextInt(3) - 1;
                }

                IntPoint point = foodBarOffsets.get(i);
                if (point == null) {
                    point = new IntPoint();
                    foodBarOffsets.set(i, point);
                }
                point.x = x;
                point.y = y;
            }
        }

        public Vector<IntPoint> healthBarOffsets(int guiTick, PlayerEntity player) {
            if (guiTick != lastGuiTick) {
                generate(guiTick, player);
                lastGuiTick = guiTick;
            }
            return healthBarOffsets;
        }

        public Vector<IntPoint> foodBarOffsets(int guiTick, PlayerEntity player) {
            if (guiTick != lastGuiTick) {
                generate(guiTick, player);
                lastGuiTick = guiTick;
            }
            return foodBarOffsets;
        }
    }

    public static class HeldFoodCache {
        @Nullable
        protected HungerTweaksFoodHelper.QueriedFoodResult result;
        public int lastGuiTick = 0;

        protected void query(PlayerEntity player) {
            ItemStack heldItem = player.getMainHandStack();
            HungerTweaksFoodHelper.QueriedFoodResult heldFood = HungerTweaksFoodHelper.query(heldItem, player);
            boolean canConsume = heldFood != null && HungerTweaksFoodHelper.canConsume(player, heldFood.modifiedFoodComponent());
            if (HungerTweaksModule.INSTANCE.showFoodValuesHudOverlayWhenOffhand() && !canConsume) {
                heldItem = player.getOffHandStack();
                heldFood = HungerTweaksFoodHelper.query(heldItem, player);
                canConsume = heldFood != null && HungerTweaksFoodHelper.canConsume(player, heldFood.modifiedFoodComponent());
            }

            if (heldItem.isEmpty() || !canConsume) {
                this.result = null;
                return;
            }
            this.result = heldFood;
        }

        public HungerTweaksFoodHelper.QueriedFoodResult result(int guiTick, PlayerEntity player) {
            if (guiTick != lastGuiTick) {
                query(player);
                lastGuiTick = guiTick;
            }
            return result;
        }
    }
}
