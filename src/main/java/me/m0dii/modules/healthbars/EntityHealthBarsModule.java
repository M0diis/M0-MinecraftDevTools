package me.m0dii.modules.healthbars;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.m0dii.modules.Module;
import me.m0dii.render.OverlayBufferSource;
import me.m0dii.render.OverlayRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EntityHealthBarsModule extends Module {
    public static final EntityHealthBarsModule INSTANCE = new EntityHealthBarsModule();

    private static final List<ColorPreset> COLOR_PRESETS = List.of(
            new ColorPreset("Green", 0xFF55FF55),
            new ColorPreset("Yellow", 0xFFFFFF55),
            new ColorPreset("Orange", 0xFFFFAA00),
            new ColorPreset("Red", 0xFFFF5555),
            new ColorPreset("Cyan", 0xFF55FFFF),
            new ColorPreset("White", 0xFFFFFFFF)
    );

    private DisplayMode displayMode = DisplayMode.ALL;
    private VisibilityMode visibilityMode = VisibilityMode.ALWAYS;
    private HealthTextFormat textFormat = HealthTextFormat.VALUE_MAX;
    private boolean showPercentage = true;
    private int renderDistance = 24;
    private int highColorIndex = 0;
    private int mediumColorIndex = 1;
    private int lowColorIndex = 3;

    private EntityHealthBarsModule() {
        super("entity_health_bars", "Entity Health Bars", false);
    }

    @Override
    public void register() {
        OverlayRenderEvents.AFTER_ENTITIES.register(context -> {
            if (!isEnabled() || isClientNull()) {
                return;
            }

            Camera camera = context.gameRenderer().mainCamera();
            PoseStack matrices = context.matrices();
            if (camera == null || matrices == null) {
                return;
            }

            OverlayBufferSource consumers = context.consumers();
            if (consumers == null) {
                consumers = OverlayBufferSource.current();
            }

            if (visibilityMode == VisibilityMode.LOOKING) {
                renderLookTarget(matrices, consumers, camera);
                return;
            }

            renderNearby(matrices, consumers, camera);
        });
    }

    private void renderLookTarget(PoseStack matrices, OverlayBufferSource consumers, Camera camera) {
        HitResult hitResult = getClient().hitResult;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) {
            return;
        }
        Entity entity = entityHitResult.getEntity();
        if (entity instanceof LivingEntity living && shouldRender(living, camera.position())) {
            renderHealthLabel(matrices, consumers, camera, living);
        }
    }

    private void renderNearby(PoseStack matrices, OverlayBufferSource consumers, Camera camera) {
        Vec3 cameraPos = camera.position();
        double maxDistanceSq = renderDistance * renderDistance;

        List<LivingEntity> entities = getClient().level.getEntities(getClient().player, getClient().player.getBoundingBox().inflate(renderDistance))
                .stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> shouldRender(entity, cameraPos))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(getClient().player)))
                .toList();

        for (LivingEntity entity : entities) {
            if (cameraPos.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) > maxDistanceSq) {
                continue;
            }
            renderHealthLabel(matrices, consumers, camera, entity);
        }
    }

    private boolean shouldRender(LivingEntity entity, Vec3 cameraPos) {
        if (entity == null || !entity.isAlive() || entity.isInvisible()) {
            return false;
        }
        if (entity == getClient().player) {
            return false;
        }
        if (cameraPos.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) > (renderDistance * renderDistance)) {
            return false;
        }
        return switch (displayMode) {
            case ALL -> true;
            case HOSTILE -> entity instanceof Enemy;
            case PASSIVE -> entity instanceof AgeableMob || entity instanceof Animal;
            case PLAYERS -> entity instanceof Player;
        };
    }

    private void renderHealthLabel(PoseStack matrices,
                                   OverlayBufferSource consumers,
                                   Camera camera,
                                   LivingEntity entity) {
        Vec3 cameraPos = camera.position();
        Vec3 renderPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() + 0.55, entity.getZ());
        double distance = cameraPos.distanceTo(renderPos);
        float scale = (float) Math.clamp(distance * 0.0025, 0.018, 0.035);

        String nameText = entity.getName().getString();
        String healthText = buildHealthText(entity);
        int color = resolveHealthColor(entity);

        matrices.pushPose();
        matrices.translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z);
        matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
        matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.scale(-scale, -scale, scale);

        Font textRenderer = getClient().font;
        Matrix4f matrix = matrices.last().pose();
        int light = 0x00F000F0;

        drawCentered(textRenderer, consumers, matrix, nameText, -18, 0xFFFFFFFF, light);
        drawCentered(textRenderer, consumers, matrix, healthText, -8, color, light);
        matrices.popPose();
    }

    private void drawCentered(Font textRenderer,
                              OverlayBufferSource consumers,
                              Matrix4f matrix,
                              String text,
                              float y,
                              int color,
                              int light) {
        int width = textRenderer.width(text);
        OverlayBufferSource.drawText(textRenderer, 
                text,
                -width / 2f,
                y,
                color,
                false,
                matrix,
                consumers,
                Font.DisplayMode.SEE_THROUGH,
                0,
                light
        );
    }

    private String buildHealthText(LivingEntity entity) {
        String bar = buildBar(entity.getHealth(), entity.getMaxHealth(), 10);
        String body = switch (textFormat) {
            case NONE -> "";
            case VALUE -> formatValue(entity.getHealth());
            case VALUE_MAX -> formatValue(entity.getHealth()) + "/" + formatValue(entity.getMaxHealth());
        };

        if (showPercentage) {
            String percent = formatValue(percent(entity.getHealth(), entity.getMaxHealth())) + "%";
            body = body.isEmpty() ? percent : body + " (" + percent + ")";
        }

        return body.isEmpty() ? bar : bar + " " + body;
    }

    private static String buildBar(float health, float maxHealth, int width) {
        if (maxHealth <= 0.0f) {
            return "[----------]";
        }
        int filled = Math.round(Math.clamp(health / maxHealth, 0.0f, 1.0f) * width);
        StringBuilder builder = new StringBuilder(width + 2);
        builder.append('[');
        for (int i = 0; i < width; i++) {
            builder.append(i < filled ? '|' : '-');
        }
        builder.append(']');
        return builder.toString();
    }

    private int resolveHealthColor(LivingEntity entity) {
        double healthPercent = percent(entity.getHealth(), entity.getMaxHealth());
        if (healthPercent <= 33.0) {
            return COLOR_PRESETS.get(lowColorIndex).argb();
        }
        if (healthPercent <= 66.0) {
            return COLOR_PRESETS.get(mediumColorIndex).argb();
        }
        return COLOR_PRESETS.get(highColorIndex).argb();
    }

    private static double percent(double health, double maxHealth) {
        if (maxHealth <= 0.0) {
            return 0.0;
        }
        return (health / maxHealth) * 100.0;
    }

    private static String formatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6) {
            return Integer.toString((int) Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Display: " + displayMode.label);
        settings.add("Visibility: " + visibilityMode.label);
        settings.add("Text: " + textFormat.label);
        settings.add("Show %: " + (showPercentage ? "ON" : "OFF"));
        settings.add("Distance: " + renderDistance);
        settings.add("Distance (+)");
        settings.add("Distance (-)");
        settings.add("High Color: " + COLOR_PRESETS.get(highColorIndex).name());
        settings.add("Mid Color: " + COLOR_PRESETS.get(mediumColorIndex).name());
        settings.add("Low Color: " + COLOR_PRESETS.get(lowColorIndex).name());
        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> displayMode = displayMode.next();
            case 1 -> visibilityMode = visibilityMode.next();
            case 2 -> textFormat = textFormat.next();
            case 3 -> showPercentage = !showPercentage;
            case 5 -> renderDistance = Math.min(64, renderDistance + 4);
            case 6 -> renderDistance = Math.max(4, renderDistance - 4);
            case 7 -> highColorIndex = cycleColor(highColorIndex);
            case 8 -> mediumColorIndex = cycleColor(mediumColorIndex);
            case 9 -> lowColorIndex = cycleColor(lowColorIndex);
            default -> {
                // Display-only setting row.
            }
        }
    }

    private int cycleColor(int current) {
        return (current + 1) % COLOR_PRESETS.size();
    }

    private enum DisplayMode {
        ALL("All"),
        HOSTILE("Hostile"),
        PASSIVE("Passive"),
        PLAYERS("Players");

        private final String label;

        DisplayMode(String label) {
            this.label = label;
        }

        private DisplayMode next() {
            DisplayMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum VisibilityMode {
        ALWAYS("Always"),
        LOOKING("Looking");

        private final String label;

        VisibilityMode(String label) {
            this.label = label;
        }

        private VisibilityMode next() {
            VisibilityMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private enum HealthTextFormat {
        NONE("Bar Only"),
        VALUE("Value"),
        VALUE_MAX("Value/Max");

        private final String label;

        HealthTextFormat(String label) {
            this.label = label;
        }

        private HealthTextFormat next() {
            HealthTextFormat[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private record ColorPreset(String name, int argb) {
    }
}
