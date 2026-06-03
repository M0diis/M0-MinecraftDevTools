package me.m0dii.modules.bridging;

import me.m0dii.utils.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

record BridgingPerspective(Vec3d position, Vec3d lookVector) {
    static BridgingPerspective resolve(MinecraftClient client, Entity player) {
        if (client == null || player == null) {
            return new BridgingPerspective(Vec3d.ZERO, Vec3d.ZERO);
        }

        if (ModConfig.bridgingPerspectiveLock == BridgingPerspectiveLock.ALWAYS_EYELINE) {
            return new BridgingPerspective(player.getEyePos(), player.getRotationVec(1.0f));
        }

        try {
            Entity cameraEntity = client.getCameraEntity();
            if (cameraEntity != null) {
                return new BridgingPerspective(cameraEntity.getEyePos(), cameraEntity.getRotationVec(1.0f));
            }
        } catch (Throwable ignored) {
        }
        return new BridgingPerspective(player.getEyePos(), player.getRotationVec(1.0f));
    }
}
