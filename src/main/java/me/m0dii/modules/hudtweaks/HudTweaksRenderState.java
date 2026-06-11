package me.m0dii.modules.hudtweaks;

import org.joml.Matrix3x2fStack;

public final class HudTweaksRenderState {

    private static final HudTweaksSettings.ElementConfig DEFAULT_CONFIG = new HudTweaksSettings.ElementConfig();
    private static final ThreadLocal<HudTweaksSettings.ElementConfig> CURRENT = ThreadLocal.withInitial(() -> DEFAULT_CONFIG);
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
        PUSHED.set(true);
        matrices.pushMatrix();
        matrices.translate(config.offsetX, config.offsetY);
        matrices.scale(config.scale, config.scale);
        return true;
    }

    public static void end(Matrix3x2fStack matrices) {
        try {
            if (PUSHED.get()) {
                matrices.popMatrix();
            }
        } finally {
            PUSHED.set(false);
            CURRENT.set(DEFAULT_CONFIG);
        }
    }

    public static float currentScale() {
        return CURRENT.get().scale;
    }

    public static float currentOpacity() {
        return CURRENT.get().opacity;
    }

    public static int multiplyAlpha(int argb) {
        float opacity = Math.clamp(currentOpacity(), 0.0f, 1.0f);
        int alpha = (argb >>> 24) & 0xFF;
        int updatedAlpha = Math.clamp((int) (alpha * opacity), 0, 255);
        return (argb & 0x00FFFFFF) | (updatedAlpha << 24);
    }
}

