package me.m0dii.modules.hungertweaks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HungerTweaksTooltipOverlayHandler {
    public static HungerTweaksTooltipOverlayHandler INSTANCE;

    static abstract class EmptyText implements Text {
        private static final List<Text> EMPTY_SIBLINGS = new ArrayList<>();

        @Override
        public Style getStyle() {
            return Style.EMPTY;
        }

        @Override
        public TextContent getContent() {
            return PlainTextContent.EMPTY;
        }

        @Override
        public List<Text> getSiblings() {
            return EMPTY_SIBLINGS;
        }
    }

    public static class FoodOverlayTextComponent extends EmptyText implements OrderedText {
        public final FoodOverlay foodOverlay;

        FoodOverlayTextComponent(FoodOverlay foodOverlay) {
            this.foodOverlay = foodOverlay;
        }

        @Override
        public OrderedText asOrderedText() {
            return this;
        }

        @Override
        public boolean accept(CharacterVisitor visitor) {
            return TextVisitFactory.visitFormatted(this, getStyle(), visitor);
        }
    }

    public static class FoodOverlay implements TooltipComponent, TooltipData {
        private final FoodComponent defaultFood;
        private final FoodComponent modifiedFood;
        private final int hungerBars;
        private final String hungerBarsText;
        private final int saturationBars;
        private final String saturationBarsText;

        FoodOverlay(FoodComponent defaultFood, FoodComponent modifiedFood) {
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
        public int getHeight(TextRenderer textRenderer) {
            return 20;
        }

        @Override
        public int getWidth(TextRenderer textRenderer) {
            int hungerBarLength = hungerBars * 9 + (hungerBarsText != null ? textRenderer.getWidth(hungerBarsText) : 0);
            int saturationBarLength = saturationBars * 7 + (saturationBarsText != null ? textRenderer.getWidth(saturationBarsText) : 0);
            return Math.max(hungerBarLength, saturationBarLength);
        }

        @Override
        public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
            if (INSTANCE != null) {
                INSTANCE.onRenderTooltip(context, this, x, y, textRenderer);
            }
        }
    }

    public static void init() {
        INSTANCE = new HungerTweaksTooltipOverlayHandler();
    }

    public void onItemTooltip(ItemStack hoveredStack, @Nullable PlayerEntity player, Item.TooltipContext context, TooltipType type, List tooltip) {
        if (hoveredStack == null || tooltip == null || !shouldShowTooltip(hoveredStack, type)) {
            return;
        }

        PlayerEntity effectivePlayer = player != null ? player : MinecraftClient.getInstance().player;
        if (effectivePlayer == null) {
            return;
        }

        HungerTweaksFoodHelper.QueriedFoodResult queriedFoodResult = HungerTweaksFoodHelper.query(hoveredStack, effectivePlayer);
        if (queriedFoodResult == null) {
            return;
        }

        FoodOverlay foodOverlay = new FoodOverlay(queriedFoodResult.defaultFoodComponent, queriedFoodResult.modifiedFoodComponent);
        if (foodOverlay.shouldRenderHungerBars()) {
            tooltip.add(new FoodOverlayTextComponent(foodOverlay));
        }
    }

    public void onRenderTooltip(DrawContext context, FoodOverlay foodOverlay, int toolTipX, int toolTipY, TextRenderer textRenderer) {
        int x = toolTipX + (foodOverlay.hungerBars - 1) * 9;
        int y = toolTipY;

        int defaultFoodHunger = foodOverlay.defaultFood.nutrition();
        int modifiedFoodHunger = foodOverlay.modifiedFood.nutrition();
        boolean rotten = HungerTweaksFoodHelper.isRotten(foodOverlay.modifiedFood);

        for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);

            if (FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i) != FoodOutline.NORMAL) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9);
            }

            boolean defaultHalf = defaultFoodHunger - 1 == i;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                    HungerTweaksTextureHelper.getFoodTexture(rotten, defaultHalf ? HungerTweaksTextureHelper.FoodType.HALF : HungerTweaksTextureHelper.FoodType.FULL),
                    x, y, 9, 9, 0.25f);

            if (modifiedFoodHunger > i) {
                boolean modifiedHalf = modifiedFoodHunger - 1 == i;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,
                        HungerTweaksTextureHelper.getFoodTexture(rotten, modifiedHalf ? HungerTweaksTextureHelper.FoodType.HALF : HungerTweaksTextureHelper.FoodType.FULL),
                        x, y, 9, 9);
            }

            x -= 9;
        }

        if (foodOverlay.hungerBarsText != null) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 18, y);
            context.getMatrices().scale(0.75f, 0.75f);
            context.drawTextWithShadow(textRenderer, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
            context.getMatrices().popMatrix();
        }

        x = toolTipX + (foodOverlay.saturationBars - 1) * 7;
        y += 10;
        float modifiedSaturationIncrement = foodOverlay.modifiedFood.saturation();
        float absoluteSaturationIncrement = Math.abs(modifiedSaturationIncrement);

        for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2) {
            float effectiveSaturationOfBar = (absoluteSaturationIncrement - i) / 2f;
            int u = effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : 28;
            int v = modifiedSaturationIncrement >= 0 ? 27 : 34;
            context.drawTexture(RenderPipelines.GUI_TEXTURED, HungerTweaksTextureHelper.MOD_ICONS, x, y, (float) u, (float) v, 7, 7, 256, 256);
            x -= 7;
        }

        if (foodOverlay.saturationBarsText != null) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 14, y);
            context.getMatrices().scale(0.75f, 0.75f);
            context.drawTextWithShadow(textRenderer, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
            context.getMatrices().popMatrix();
        }
    }

    private boolean shouldShowTooltip(ItemStack hoveredStack, TooltipType type) {
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        return InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
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
