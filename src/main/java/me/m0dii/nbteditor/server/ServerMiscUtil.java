package me.m0dii.nbteditor.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class ServerMiscUtil {

    public static Entity createEntity(EntityType<?> entityType, World world) {
        return entityType.create(world, SpawnReason.COMMAND);
    }

    public static boolean hasPermissionLevel(PlayerEntity player, int level) {
        return player.hasPermissionLevel(level);
    }

}
