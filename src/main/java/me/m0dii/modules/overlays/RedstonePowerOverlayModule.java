package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class RedstonePowerOverlayModule extends Module {

    public static final RedstonePowerOverlayModule INSTANCE = new RedstonePowerOverlayModule();

    private RedstonePowerOverlayModule() {
        super("redstone_overlay", "Redstone overlay", false);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(getAfterEntities());

        registerPressedKeybind("key.m0-dev-tools.toggle_redstone_overlay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, client -> {
            toggleEnabled();
        });
    }

    private WorldRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled()) {
                return;
            }

            if (getClient().player == null || getClient().world == null) {
                return;
            }

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) {
                return;
            }
            TextRenderer textRenderer = getClient().textRenderer;
            Vec3d cameraPos = context.camera().getPos();
            BlockPos playerPos = getClient().player.getBlockPos();

            VertexConsumerProvider vcp = context.consumers();
            if (vcp == null) {
                vcp = getClient().getBufferBuilders().getEntityVertexConsumers();
            }

            for (int x = -ModConfig.overlayXZradius; x <= ModConfig.overlayXZradius; x++) {
                for (int y = -ModConfig.overlayYradius; y <= ModConfig.overlayYradius; y++) {
                    for (int z = -ModConfig.overlayXZradius; z <= ModConfig.overlayXZradius; z++) {
                        BlockPos pos = playerPos.add(x, y, z);
                        BlockState state = getClient().world.getBlockState(pos);
                        if (state.isAir()) {
                            continue;
                        }

                        // Only render on redstone wire blocks (exact nodes)
                        if (!(state.getBlock() instanceof RedstoneWireBlock)) {
                            continue;
                        }

                        // Read the wire's POWER property directly
                        int wirePower;
                        try {
                            wirePower = state.get(RedstoneWireBlock.POWER);
                        } catch (Exception e) {
                            continue;
                        }
                        if (wirePower <= 0) {
                            continue;
                        }

                        Vec3d blockTop = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.35);
                        if (cameraPos.squaredDistanceTo(blockTop) > ModConfig.overlayXZradius * ModConfig.overlayXZradius) {
                            continue;
                        }

                        int g = (int) Math.round((wirePower / 15.0) * 255.0);
                        int color = (0xFF << 16) | (g << 8);

                        matrices.push();
                        matrices.translate(blockTop.x - cameraPos.x, blockTop.y - cameraPos.y, blockTop.z - cameraPos.z);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
                        float scale = 0.04F;
                        matrices.scale(scale, scale, scale);

                        String s = Integer.toString(wirePower);
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
