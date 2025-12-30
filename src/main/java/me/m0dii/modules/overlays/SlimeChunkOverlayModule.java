package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import me.m0dii.utils.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class SlimeChunkOverlayModule extends Module {

    public static final SlimeChunkOverlayModule INSTANCE = new SlimeChunkOverlayModule();

    private SlimeChunkOverlayModule() {
        super("slime_chunk_overlay", "Slime chunks overlay", false);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(getAfterEntities());

        registerPressedKeybind("key.m0-dev-tools.toggle_slime_overlay", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, client -> {
            toggleEnabled();
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal("Slime chunk overlay: " + (isEnabled() ? "ON" : "OFF")), true);
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

            long seed;
            MinecraftServer server = client.getServer();
            if (server != null) {
                seed = server.getSaveProperties().getGeneratorOptions().getSeed();
            } else {
                return; // Seed not available
            }

            MatrixStack matrices = context.matrixStack();
            if (matrices == null) {
                return;
            }
            Vec3d cameraPos = context.camera().getPos();
            ChunkPos playerChunk = new ChunkPos(client.player.getBlockPos());

            VertexConsumerProvider vcp = context.consumers();
            if (vcp == null) {
                vcp = client.getBufferBuilders().getEntityVertexConsumers();
            }

            int chunkRadius = Math.max(1, ModConfig.overlayXZradius / 16);
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    ChunkPos chunkPos = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
                    if (isSlimeChunk(seed, chunkPos)) {
                        renderChunkBorder(matrices, cameraPos, chunkPos, vcp);
                    }
                }
            }
        };
    }

    private static boolean isSlimeChunk(long seed, ChunkPos pos) {
        int x = pos.x;
        int z = pos.z;
        return new Random(seed + (x * x * 0x4c1906L) + (x * 0x5ac0dbL) + (z * z * 0x4307a7L) + (z * 0x5f24fL) ^ 0x3ad8025fL).nextInt(10) == 0;
    }

    private static void renderChunkBorder(MatrixStack matrices, Vec3d cameraPos, ChunkPos chunkPos, VertexConsumerProvider vcp) {
        matrices.push();

        // Chunk boundaries
        float minX = (float) (chunkPos.getStartX() - cameraPos.x);
        float minZ = (float) (chunkPos.getStartZ() - cameraPos.z);
        float maxX = (float) (chunkPos.getEndX() + 1 - cameraPos.x);
        float maxZ = (float) (chunkPos.getEndZ() + 1 - cameraPos.z);

        // Use the player's Y position as reference
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        double playerY = client.player.getY();
        float minY = (float) (playerY - 5f - cameraPos.y);
        float maxY = (float) (playerY + 5f - cameraPos.y);

        VertexConsumer vertexConsumer = vcp.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Draw vertical lines at corners using Vec3d endpoints
        drawLine(vertexConsumer, matrix, new Vec3d(minX, minY, minZ), new Vec3d(minX, maxY, minZ));
        drawLine(vertexConsumer, matrix, new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ));
        drawLine(vertexConsumer, matrix, new Vec3d(minX, minY, maxZ), new Vec3d(minX, maxY, maxZ));
        drawLine(vertexConsumer, matrix, new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ));

        matrices.pop();
    }

    private static void drawLine(VertexConsumer vertexConsumer,
                                 Matrix4f matrix,
                                 Vec3d a,
                                 Vec3d b) {
        // Green color for slime chunks (0.0f red, 1.0f green, 0.0f blue, 1.0f alpha)
        vertexConsumer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(0.0f, 1.0f, 0.0f, 1.0f).normal(0.0f, 0.0f, 0.0f);
        vertexConsumer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(0.0f, 1.0f, 0.0f, 1.0f).normal(0.0f, 0.0f, 0.0f);
    }
}
