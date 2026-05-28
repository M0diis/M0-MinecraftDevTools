package me.m0dii.modules.overlays;

import me.m0dii.modules.Module;
import me.m0dii.utils.DrawUtil;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SlimeChunkOverlayModule extends Module {

    public static final SlimeChunkOverlayModule INSTANCE = new SlimeChunkOverlayModule();

    private int XZ_RADIUS = 16;

    private SlimeChunkOverlayModule() {
        super("slime_chunk_overlay", "Slime Chunks Overlay", false);
    }

    @Override
    public void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(getAfterEntities());

        registerPressedKeybind(KeybindCatalog.SLIME_OVERLAY_TOGGLE.translationKey(),
                InputUtil.Type.KEYSYM,
                KeybindCatalog.SLIME_OVERLAY_TOGGLE.defaultKey(),
                client -> toggleEnabled());
    }

    private WorldRenderEvents.@NotNull AfterEntities getAfterEntities() {
        return context -> {
            if (!isEnabled() || isClientNull()) {
                return;
            }

            long seed;
            MinecraftServer server = getClient().getServer();
            if (server != null) {
                seed = server.getSaveProperties().getGeneratorOptions().getSeed();
            } else {
                return; // Seed not available
            }

            MatrixStack matrices = context.matrices();
            Camera camera = context.gameRenderer().getCamera();
            if (matrices == null || camera == null) {
                return;
            }

            Vec3d cameraPos = camera.getCameraPos();
            ChunkPos playerChunk = new ChunkPos(getClient().player.getBlockPos());

            VertexConsumerProvider vcp = context.consumers();
            if (vcp == null) {
                vcp = getClient().getBufferBuilders().getEntityVertexConsumers();
            }

            int chunkRadius = Math.max(1, XZ_RADIUS / 16);
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

    @Override
    public List<String> getSettingsDisplay() {
        List<String> settings = new ArrayList<>();
        settings.add("Toggle: " + (isEnabled() ? "ON" : "OFF"));
        settings.add("XZ radius : " + XZ_RADIUS);
        settings.add("XZ+");
        settings.add("XZ-");

        return settings;
    }

    @Override
    public void onSettingSelected(int settingIndex) {
        switch (settingIndex) {
            case 0 -> toggleEnabled();
            case 3 -> XZ_RADIUS++;
            case 4 -> XZ_RADIUS = Math.max(1, XZ_RADIUS - 1);
            default -> {
                // Nothing
            }
        }
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

        VertexConsumer vertexConsumer = vcp.getBuffer(RenderLayers.LINES);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        DrawUtil.drawLine(vertexConsumer, matrix, minX, minY, minZ, minX, maxY, minZ,
                0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        DrawUtil.drawLine(vertexConsumer, matrix, maxX, minY, minZ, maxX, maxY, minZ,
                0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        DrawUtil.drawLine(vertexConsumer, matrix, minX, minY, maxZ, minX, maxY, maxZ,
                0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
        DrawUtil.drawLine(vertexConsumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ,
                0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);

        matrices.pop();
    }
}
