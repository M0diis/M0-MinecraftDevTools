package me.m0dii.modules.freecam;


import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class CameraUtils {

    private CameraUtils() {
    }

    @Setter
    private static boolean freeCameraSpectator;

    public static boolean getFreeCameraSpectator() {
        return freeCameraSpectator;
    }

    public static boolean shouldPreventPlayerInputs() {
        return FreecamModule.INSTANCE.isEnabled() && true;
    }

    public static boolean shouldPreventPlayerMovement() {
        return FreecamModule.INSTANCE.isEnabled() && true;
    }

    public static void updateCameraRotations(float yawChange, float pitchChange) {
        CameraEntity camera = CameraEntity.getCamera();

        if (camera != null) {
            camera.updateCameraRotations(yawChange, pitchChange);
        }
    }

    public static void markChunksForRebuild(int chunkX, int chunkZ, int lastChunkX, int lastChunkZ) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null || (chunkX == lastChunkX && chunkZ == lastChunkZ)) {
            return;
        }

        final int viewDistance = mc.options.getViewDistance().getValue();

        if (chunkX != lastChunkX) {
            final int minCX = chunkX > lastChunkX ? lastChunkX + viewDistance : chunkX - viewDistance;
            final int maxCX = chunkX > lastChunkX ? chunkX + viewDistance : lastChunkX - viewDistance;

            for (int cx = minCX; cx <= maxCX; ++cx) {
                for (int cz = chunkZ - viewDistance; cz <= chunkZ + viewDistance; ++cz) {
                    if (isClientChunkLoaded(mc.world, cx, cz)) {
                        markChunkForReRender(mc.worldRenderer, cx, cz);
                    }
                }
            }
        }

        if (chunkZ != lastChunkZ) {
            final int minCZ = chunkZ > lastChunkZ ? lastChunkZ + viewDistance : chunkZ - viewDistance;
            final int maxCZ = chunkZ > lastChunkZ ? chunkZ + viewDistance : lastChunkZ - viewDistance;

            for (int cz = minCZ; cz <= maxCZ; ++cz) {
                for (int cx = chunkX - viewDistance; cx <= chunkX + viewDistance; ++cx) {
                    if (isClientChunkLoaded(mc.world, cx, cz)) {
                        markChunkForReRender(mc.worldRenderer, cx, cz);
                    }
                }
            }
        }
    }

    public static void markChunksForRebuildOnDeactivation(int lastChunkX, int lastChunkZ) {
        Entity entity = getCameraEntity();
        MinecraftClient client = MinecraftClient.getInstance();
        final int viewDistance = client.options.getViewDistance().getValue();
        final int chunkX = MathHelper.floor(entity.getX() / 16.0) >> 4;
        final int chunkZ = MathHelper.floor(entity.getZ() / 16.0) >> 4;

        final int minCameraCX = lastChunkX - viewDistance;
        final int maxCameraCX = lastChunkX + viewDistance;
        final int minCameraCZ = lastChunkZ - viewDistance;
        final int maxCameraCZ = lastChunkZ + viewDistance;
        final int minCX = chunkX - viewDistance;
        final int maxCX = chunkX + viewDistance;
        final int minCZ = chunkZ - viewDistance;
        final int maxCZ = chunkZ + viewDistance;

        for (int cz = minCZ; cz <= maxCZ; ++cz) {
            for (int cx = minCX; cx <= maxCX; ++cx) {
                // Mark all chunks that were not in free camera range
                if ((cx < minCameraCX || cx > maxCameraCX || cz < minCameraCZ || cz > maxCameraCZ) &&
                        isClientChunkLoaded(client.world, cx, cz)) {
                    markChunkForReRender(client.worldRenderer, cx, cz);
                }
            }
        }
    }

    public static void markChunkForReRender(WorldRenderer renderer, int chunkX, int chunkZ) {
        for (int cy = 0; cy < 16; ++cy) {
            int minX = chunkX << 4;
            int minY = cy << 4;
            int minZ = chunkZ << 4;

            int maxX = minX + 16;
            int maxY = minY + 16;
            int maxZ = minZ + 16;

            renderer.scheduleBlockRenders(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static boolean isClientChunkLoaded(ClientWorld world, int chunkX, int chunkZ) {
        return world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public static Entity getCameraEntity() {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity entity = client.getCameraEntity();

        if (entity == null) {
            entity = client.player;
        }

        return entity;
    }
}