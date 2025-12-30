package me.m0dii.nbteditor.multiversion;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.m0dii.nbteditor.util.MiscUtil;
import me.m0dii.nbteditor.util.Reflection;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class DrawableHelper {

    private DrawableHelper() {
    }

    private static final Cache<MatrixStack, DrawContext> drawContexts = CacheBuilder.newBuilder().weakKeys().weakValues().build();

    public static MatrixStack getMatrices(DrawContext context) {
        MatrixStack matrices = context.getMatrices();
        drawContexts.put(matrices, context);
        return matrices;
    }

    public static DrawContext getDrawContext(MatrixStack matrices) {
        return drawContexts.getIfPresent(matrices);
    }

    public static void superRender(Class<?> callerClass, Drawable caller, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        try {
            Class<?> matrixType = DrawContext.class;
            DrawContext matrixValue = getDrawContext(matrices);

            MethodType methodType = MethodType.methodType(void.class, matrixType, int.class, int.class, float.class);
            String methodName = Reflection.getMethodName(Drawable.class, "method_25394", methodType);
            MethodHandles.privateLookupIn(callerClass, MethodHandles.lookup()).findSpecial(callerClass.getSuperclass(),
                    methodName, methodType, callerClass).invokeWithArguments(caller, matrixValue, mouseX, mouseY, delta);
        } catch (Throwable e) {
            throw new RuntimeException("Error calling super.render (" + callerClass.getName() + ")", e);
        }
    }

    public static void render(Drawable caller, MatrixStack matrices, int mouseX, int mouseY, float delta) {
        caller.render(DrawableHelper.getDrawContext(matrices), mouseX, mouseY, delta);
    }

    public static VertexConsumerProvider.Immediate getVertexConsumerProvider() {
        return MiscUtil.client.gameRenderer.buffers.getEntityVertexConsumers();
    }

    public static void fill(MatrixStack matrices, int x1, int y1, int x2, int y2, int color) {
        getDrawContext(matrices).fill(RenderLayer.getGuiOverlay(), x1, y1, x2, y2, color);
    }

    public static void drawText(MatrixStack matrices, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            drawTextWithShadow(matrices, textRenderer, text, x, y, color);
        } else {
            drawTextWithoutShadow(matrices, textRenderer, text, x, y, color);
        }
    }

    public static void drawTextWithoutShadow(MatrixStack matrices, TextRenderer textRenderer, Text text, int x, int y, int color) {
        getDrawContext(matrices).drawText(textRenderer, text, x, y, color, false);
    }

    public static void drawTextWithShadow(MatrixStack matrices, TextRenderer textRenderer, Text text, int x, int y, int color) {
        getDrawContext(matrices).drawTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawCenteredTextWithShadow(MatrixStack matrices, TextRenderer textRenderer, Text text, int x, int y, int color) {
        getDrawContext(matrices).drawCenteredTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawTexture(MatrixStack matrices, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        getDrawContext(matrices).drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void drawTexture(MatrixStack matrices, Identifier texture, int x, int y, float u, float v, int width, int height) {
        drawTexture(matrices, texture, x, y, u, v, width, height, 256, 256);
    }

    public static void renderTooltip(MatrixStack matrices, Text text, int x, int y) {
        getDrawContext(matrices).drawTooltip(MiscUtil.client.textRenderer, text, x, y);
    }

    public static void renderTooltip(MatrixStack matrices, List<OrderedText> lines, int x, int y) {
        getDrawContext(matrices).drawOrderedTooltip(MiscUtil.client.textRenderer, lines, x, y);
    }

    public static void renderItem(MatrixStack matrices, float zOffset, boolean setScreenZOffset, ItemStack item, int x, int y) {
        TextRenderer textRenderer = MiscUtil.client.textRenderer;
        DrawContext context = getDrawContext(matrices);
        context.drawItem(item, x, y);
        context.drawStackOverlay(textRenderer, item, x, y);
    }

    public static void renderBackground(Screen screen, MatrixStack matrices) {
        int[] mousePos = MiscUtil.getMousePos();
        if (MiscUtil.client.world == null) {
            screen.renderBackground(getDrawContext(matrices), mousePos[0], mousePos[1], MVMisc.getTickDelta());
        } else {
            screen.renderInGameBackground(getDrawContext(matrices));
        }
    }

    public static void drawSlotHighlight(MatrixStack matrices, int x, int y, int color) {
        getDrawContext(matrices).fillGradient(RenderLayer.getGuiOverlay(), x, y, x + 16, y + 16, color, color, 0);
    }

    public static void enableScissor(MatrixStack matrices, int x, int y, int width, int height) {
        getDrawContext(matrices).enableScissor(x, y, x + width, y + height);
    }

    public static void disableScissor(MatrixStack matrices) {
        getDrawContext(matrices).disableScissor();
    }

}
