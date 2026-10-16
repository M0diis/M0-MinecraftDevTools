package me.m0dii.modules.hungertweaks;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HungerTweaksTooltipOverlayHandler {
    public static HungerTweaksTooltipOverlayHandler INSTANCE;

    static abstract class EmptyText implements Component {
        private static final List<Component> EMPTY_SIBLINGS = new ArrayList<>();

        @Override
        public Style getStyle() {
            return Style.EMPTY;
        }

        @Override
        public ComponentContents getContents() {
            return PlainTextContents.EMPTY;
        }

        @Override
        public List<Component> getSiblings() {
            return EMPTY_SIBLINGS;
        }
    }

    public static class FoodOverlayTextComponent extends EmptyText implements FormattedCharSequence {
        public final FoodOverlay foodOverlay;

        FoodOverlayTextComponent(FoodOverlay foodOverlay) {
            this.foodOverlay = foodOverlay;
        }

        @Override
        public FormattedCharSequence getVisualOrderText() {
            return this;
        }

        @Override
        public boolean accept(FormattedCharSink visitor) {
            return StringDecomposer.iterateFormatted(this, getStyle(), visitor);
        }
    }

    public static class FoodOverlay implements ClientTooltipComponent, TooltipComponent {
        private final FoodProperties defaultFood;
        private final FoodProperties modifiedFood;
        private final int hungerBars;
        private final String hungerBarsText;
        private final int saturationBars;
        private final String saturationBarsText;

        FoodOverlay(FoodProperties defaultFood, FoodProperties modifiedFood) {
            this.defaultFood = defaultFood;
            this.modifiedFood = modifiedFood;

            int biggestHunger = Math.max(defaultFood.nutrition(), modifiedFood.nutrition());
            float biggestSaturationIncrement = Math.max(defaultFood.saturation(), modifiedFood.saturation());

            int localHungerBars = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
            String localHungerBarsText = null;
            if (localHungerBars > 10) {
                localHungerBarsText = "x" + ((biggestHunger < 0 ? -1 : 1) * localHungerBars);
                localHungerBars = 1;
            }

            int localSaturationBars = (int) Math.ceil(Math.abs(biggestSaturationIncrement) / 2f);
            String localSaturationBarsText = null;
            if (localSaturationBars > 10 || localSaturationBars == 0) {
                localSaturationBarsText = "x" + ((biggestSaturationIncrement < 0 ? -1 : 1) * localSaturationBars);
                localSaturationBars = 1;
            }

            this.hungerBars = localHungerBars;
            this.hungerBarsText = localHungerBarsText;
            this.saturationBars = localSaturationBars;
            this.saturationBarsText = localSaturationBarsText;
        }

        boolean shouldRenderHungerBars() {
            return hungerBars > 0;
        }

        @Override
        public int getHeight(Font textRenderer) {
            return 20;
        }

        @Override
        public int getWidth(Font textRenderer) {
            int hungerBarLength = hungerBars * 9 + (hungerBarsText != null ? textRenderer.width(hungerBarsText) : 0);
            int saturationBarLength = saturationBars * 7 + (saturationBarsText != null ? textRenderer.width(saturationBarsText) : 0);
            return Math.max(hungerBarLength, saturationBarLength);
        }

        @Override
        public void extractImage(Font textRenderer, int x, int y, int width, int height, GuiGraphicsExtractor context) {
            if (INSTANCE != null) {
                INSTANCE.onRenderTooltip(context, this, x, y, textRenderer);
            }
        }
    }

    public static void init() {
        INSTANCE = new HungerTweaksTooltipOverlayHandler();
    }

    public void onItemTooltip(ItemStack hoveredStack, @Nullable Player player, Item.TooltipContext context, TooltipFlag type, List tooltip) {
        if (hoveredStack == null || tooltip == null || !shouldShowTooltip(hoveredStack, type)) {
            return;
        }

        Player effectivePlayer = player != null ? player : Minecraft.getInstance().player;
        if (effectivePlayer == null) {
            return;
        }

        HungerTweaksFoodHelper.QueriedFoodResult queriedFoodResult = HungerTweaksFoodHelper.query(hoveredStack, effectivePlayer);
        if (queriedFoodResult == null) {
            return;
        }

        FoodOverlay foodOverlay = new FoodOverlay(queriedFoodResult.defaultFoodComponent(), queriedFoodResult.modifiedFoodComponent());
        if (foodOverlay.shouldRenderHungerBars()) {
            tooltip.add(new FoodOverlayTextComponent(foodOverlay));
        }
    }

    public void onRenderTooltip(GuiGraphicsExtractor context, FoodOverlay foodOverlay, int toolTipX, int toolTipY, Font textRenderer) {
        int x = toolTipX + (foodOverlay.hungerBars - 1) * 9;
        int y = toolTipY;

        int defaultFoodHunger = foodOverlay.defaultFood.nutrition();
        int modifiedFoodHunger = foodOverlay.modifiedFood.nutrition();
        boolean rotten = HungerTweaksFoodHelper.isRotten(foodOverlay.modifiedFood);

        for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2) {
            context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);

            if (FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i) != FoodOutline.NORMAL) {
                context.blitSprite(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9);
            }

            boolean defaultHalf = defaultFoodHunger - 1 == i;
            context.blitSprite(RenderPipelines.GUI_TEXTURED,
                    HungerTweaksTextureHelper.getFoodTexture(rotten, defaultHalf ? HungerTweaksTextureHelper.FoodType.HALF : HungerTweaksTextureHelper.FoodType.FULL),
                    x, y, 9, 9, 0.25f);

            if (modifiedFoodHunger > i) {
                boolean modifiedHalf = modifiedFoodHunger - 1 == i;
                context.blitSprite(RenderPipelines.GUI_TEXTURED,
                        HungerTweaksTextureHelper.getFoodTexture(rotten, modifiedHalf ? HungerTweaksTextureHelper.FoodType.HALF : HungerTweaksTextureHelper.FoodType.FULL),
                        x, y, 9, 9);
            }

            x -= 9;
        }

        if (foodOverlay.hungerBarsText != null) {
            context.pose().pushMatrix();
            context.pose().translate(x + 18, y);
            context.pose().scale(0.75f, 0.75f);
            context.text(textRenderer, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
            context.pose().popMatrix();
        }

        x = toolTipX + (foodOverlay.saturationBars - 1) * 7;
        y += 10;
        float modifiedSaturationIncrement = foodOverlay.modifiedFood.saturation();
        float absoluteSaturationIncrement = Math.abs(modifiedSaturationIncrement);

        for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2) {
            float effectiveSaturationOfBar = (absoluteSaturationIncrement - i) / 2f;
            int u = effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : 28;
            int v = modifiedSaturationIncrement >= 0 ? 27 : 34;
            context.blit(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.MOD_ICONS, x, y, (float) u, (float) v, 7, 7, 256, 256);
            x -= 7;
        }

        if (foodOverlay.saturationBarsText != null) {
            context.pose().pushMatrix();
            context.pose().translate(x + 14, y);
            context.pose().scale(0.75f, 0.75f);
            context.text(textRenderer, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
            context.pose().popMatrix();
        }
    }

    private boolean shouldShowTooltip(ItemStack hoveredStack, TooltipFlag type) {
        if (!HungerTweaksModule.INSTANCE.isEnabled() || hoveredStack.isEmpty()) {
            return false;
        }

        boolean showTooltip = HungerTweaksModule.INSTANCE.showFoodValuesInTooltip() && isShiftKeyDown();
        if (HungerTweaksModule.INSTANCE.showFoodValuesInTooltipAlways()) {
            showTooltip = true;
        }
        return showTooltip && HungerTweaksFoodHelper.isFood(hoveredStack);
    }

    private static boolean isShiftKeyDown() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    private enum FoodOutline {
        NEGATIVE,
        EXTRA,
        NORMAL,
        PARTIAL,
        MISSING;

        static FoodOutline get(int modifiedFoodHunger, int defaultFoodHunger, int index) {
            if (modifiedFoodHunger < 0) {
                return NEGATIVE;
            }
            if (modifiedFoodHunger > defaultFoodHunger && defaultFoodHunger <= index) {
                return EXTRA;
            }
            if (modifiedFoodHunger > index + 1 || defaultFoodHunger == modifiedFoodHunger) {
                return NORMAL;
            }
            if (modifiedFoodHunger == index + 1) {
                return PARTIAL;
            }
            return MISSING;
        }
    }
}
