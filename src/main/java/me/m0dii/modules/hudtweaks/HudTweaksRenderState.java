package me.m0dii.modules.hudtweaks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2fStack;

public final class HudTweaksRenderState {

    private static final HudTweaksSettings.ElementConfig DEFAULT_CONFIG = new HudTweaksSettings.ElementConfig();
    private static final ThreadLocal<HudTweaksSettings.ElementConfig> CURRENT = ThreadLocal.withInitial(() -> DEFAULT_CONFIG);
    private static final ThreadLocal<HudTweaksSettings.ElementType> CURRENT_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PUSHED = ThreadLocal.withInitial(() -> false);

    private HudTweaksRenderState() {
    }

    public static boolean begin(HudTweaksSettings.ElementType type, Matrix3x2fStack matrices) {
        if (!HudTweaksModule.INSTANCE.isEnabled()) {
            return false;
        }

        HudTweaksSettings.ElementConfig config = HudTweaksSettings.getElement(type);
        if (config == null || !config.display) {
            return false;
        }

        CURRENT.set(config);
        CURRENT_TYPE.set(type);
        PUSHED.set(true);
        matrices.pushMatrix();
        applyTransform(type, config, matrices);
        return true;
    }

    private static void applyTransform(HudTweaksSettings.ElementType type,
                                       HudTweaksSettings.ElementConfig config,
                                       Matrix3x2fStack matrices) {
        float scale = Math.max(0.01f, config.scale);
        if (!usesScenePivot(type)) {
            matrices.translate(config.offsetX, config.offsetY);
            matrices.scale(scale, scale);
            return;
        }

        Window window = MinecraftClient.getInstance().getWindow();
        float pivotX = scenePivotX(type, window.getScaledWidth());
        float pivotY = scenePivotY(type, window.getScaledHeight());
        matrices.translate(config.offsetX, config.offsetY);
        matrices.translate(pivotX, pivotY);
        matrices.scale(scale, scale);
        matrices.translate(-pivotX, -pivotY);
    }

    private static boolean usesScenePivot(HudTweaksSettings.ElementType type) {
        return type == HudTweaksSettings.ElementType.HOTBAR_GROUP;
    }

    private static float scenePivotX(HudTweaksSettings.ElementType type, int width) {
        if (type == HudTweaksSettings.ElementType.HOTBAR_GROUP) {
            return width / 2.0f;
        }
        return 0.0f;
    }

    private static float scenePivotY(HudTweaksSettings.ElementType type, int height) {
        if (type == HudTweaksSettings.ElementType.HOTBAR_GROUP) {
            return height;
        }
        return 0.0f;
    }

    public static void end(Matrix3x2fStack matrices) {
        try {
            if (PUSHED.get()) {
                matrices.popMatrix();
            }
        } finally {
            PUSHED.set(false);
            CURRENT.set(DEFAULT_CONFIG);
            CURRENT_TYPE.remove();
        }
    }

    public static float currentScale() {
        return CURRENT.get().scale;
    }

    public static float currentOpacity() {
        return CURRENT.get().opacity;
    }

    public static boolean compensateCoordinates() {
        HudTweaksSettings.ElementType type = CURRENT_TYPE.get();
        return type == null || !usesScenePivot(type);
    }

    public static int multiplyAlpha(int argb) {
        float opacity = Math.clamp(currentOpacity(), 0.0f, 1.0f);
        int alpha = (argb >>> 24) & 0xFF;
        int updatedAlpha = Math.clamp((int) (alpha * opacity), 0, 255);
        return (argb & 0x00FFFFFF) | (updatedAlpha << 24);
    }
}

