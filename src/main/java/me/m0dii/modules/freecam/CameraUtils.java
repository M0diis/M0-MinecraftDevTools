package me.m0dii.modules.freecam;


import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;
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
        return FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController();
    }

    public static boolean shouldPreventPlayerMovement() {
        return FreecamModule.INSTANCE.isEnabled() || CameraEntity.hasController();
    }

    public static void updateCameraRotations(float yawChange, float pitchChange) {
        if (CameraEntity.blocksManualControl()) {
            return;
        }
        CameraEntity camera = CameraEntity.getCamera();

        if (camera != null) {
            camera.updateCameraRotations(yawChange, pitchChange);
        }
    }

    public static void markChunksForRebuild(int chunkX, int chunkZ, int lastChunkX, int lastChunkZ) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || (chunkX == lastChunkX && chunkZ == lastChunkZ)) {
            return;
        }

        final int viewDistance = mc.options.renderDistance().get();

        if (chunkX != lastChunkX) {
            final int minCX = chunkX > lastChunkX ? lastChunkX + viewDistance : chunkX - viewDistance;
            final int maxCX = chunkX > lastChunkX ? chunkX + viewDistance : lastChunkX - viewDistance;

            for (int cx = minCX; cx <= maxCX; ++cx) {
                for (int cz = chunkZ - viewDistance; cz <= chunkZ + viewDistance; ++cz) {
                    if (isClientChunkLoaded(mc.level, cx, cz)) {
                        markChunkForReRender(mc.levelRenderer, cx, cz);
                    }
                }
            }
        }

        if (chunkZ != lastChunkZ) {
            final int minCZ = chunkZ > lastChunkZ ? lastChunkZ + viewDistance : chunkZ - viewDistance;
            final int maxCZ = chunkZ > lastChunkZ ? chunkZ + viewDistance : lastChunkZ - viewDistance;

            for (int cz = minCZ; cz <= maxCZ; ++cz) {
                for (int cx = chunkX - viewDistance; cx <= chunkX + viewDistance; ++cx) {
                    if (isClientChunkLoaded(mc.level, cx, cz)) {
                        markChunkForReRender(mc.levelRenderer, cx, cz);
                    }
                }
            }
        }
    }

    public static void markChunksForRebuildOnDeactivation(int lastChunkX, int lastChunkZ) {
        Entity entity = getCameraEntity();
        Minecraft client = Minecraft.getInstance();
        if (entity == null || client.level == null) {
            return;
        }
        final int viewDistance = client.options.renderDistance().get();
        final int chunkX = Mth.floor(entity.getX()) >> 4;
        final int chunkZ = Mth.floor(entity.getZ()) >> 4;

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
                        isClientChunkLoaded(client.level, cx, cz)) {
                    markChunkForReRender(client.levelRenderer, cx, cz);
                }
            }
        }
    }

    public static void markChunkForReRender(@NotNull LevelRenderer renderer, int chunkX, int chunkZ) {
        for (int cy = 0; cy < 16; ++cy) {
            int minX = chunkX << 4;
            int minY = cy << 4;
            int minZ = chunkZ << 4;

            int maxX = minX + 16;
            int maxY = minY + 16;
            int maxZ = minZ + 16;

            Minecraft.getInstance().levelExtractor.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public static boolean isClientChunkLoaded(ClientLevel world, int chunkX, int chunkZ) {
        return world.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public static Entity getCameraEntity() {
        Minecraft client = Minecraft.getInstance();
        Entity entity = client.getCameraEntity();

        if (entity == null) {
            entity = client.player;
        }

        return entity;
    }
}
