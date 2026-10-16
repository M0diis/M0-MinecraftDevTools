package me.m0dii.modules.bridging;

import me.m0dii.utils.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

record BridgingPerspective(Vec3 position, Vec3 lookVector) {
    static BridgingPerspective resolve(Minecraft client, Entity player) {
        if (client == null || player == null) {
            return new BridgingPerspective(Vec3.ZERO, Vec3.ZERO);
        }

        if (ModConfig.bridgingPerspectiveLock == BridgingPerspectiveLock.ALWAYS_EYELINE) {
            return new BridgingPerspective(player.getEyePosition(), player.getViewVector(1.0f));
        }

        try {
            Entity cameraEntity = client.getCameraEntity();
            if (cameraEntity != null) {
                return new BridgingPerspective(cameraEntity.getEyePosition(), cameraEntity.getViewVector(1.0f));
            }
        } catch (Throwable ignored) {
        }
        return new BridgingPerspective(player.getEyePosition(), player.getViewVector(1.0f));
    }
}
