package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class LightLevelOverlayModule extends Module {
    private static final int XZ_RADIUS = 16;
    private static final int Y_RADIUS = 4;

    public static final LightLevelOverlayModule INSTANCE = new LightLevelOverlayModule();

    private LightLevelOverlayModule() {
        super("light_overlay", "Light Overlay", false);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(getAfterEntities());

        registerPressedKeybind("key.m0-dev-tools.toggle_light_overlay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, client -> {
            toggleEnabled();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Light overlay: " + (isEnabled() ? "ON" : "OFF")), true);
            }
            setEnabled(isEnabled());
        });
    }

    private WorldRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) {
                return;
            }

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) {
                return;
            }

            TextRenderer textRenderer = client.textRenderer;
            Vec3d cameraPos = context.camera().getPos();
            BlockPos playerPos = client.player.getBlockPos();

            VertexConsumerProvider vcp = context.consumers();
            if (vcp == null) {
                vcp = client.getBufferBuilders().getEntityVertexConsumers();
            }

            for (int x = -XZ_RADIUS; x <= XZ_RADIUS; x++) {
                for (int y = -Y_RADIUS; y <= Y_RADIUS; y++) {
                    for (int z = -XZ_RADIUS; z <= XZ_RADIUS; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        var state = client.world.getBlockState(pos);
                        if (state.isAir()) {
                            continue;
                        }
                        if (state.getLuminance() > 0) {
                            continue;
                        }
                        var above = client.world.getBlockState(pos.up());
                        boolean topExposed = above.isAir() || above.getCollisionShape(client.world, pos.up()).isEmpty();
                        if (!topExposed) {
                            continue;
                        }
                        int light = client.world.getLightLevel(pos.up());
                        if (light <= 0) {
                            continue;
                        }

                        Vec3d blockTop = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.005, pos.getZ() + 0.5);
                        double distSq = cameraPos.squaredDistanceTo(blockTop);
                        if (distSq > XZ_RADIUS * XZ_RADIUS) {
                            continue;
                        }

                        int color = (light < 8) ? 0xFF0000 : 0x00FF00;

                        matrices.push();
                        matrices.translate(blockTop.x - cameraPos.x, blockTop.y - cameraPos.y, blockTop.z - cameraPos.z);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
                        float scale = 0.04F;
                        matrices.scale(scale, scale, scale);

                        String s = Integer.toString(light);
                        int w = textRenderer.getWidth(s);
                        Matrix4f model = matrices.peek().getPositionMatrix();
                        int fullBright = 0x00F000F0;

                        textRenderer.draw(s, -w / 2f, 0, color, true, model, vcp, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, fullBright);

                        matrices.pop();
                    }
                }
            }
        };
    }
}
