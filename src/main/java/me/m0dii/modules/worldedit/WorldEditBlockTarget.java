package me.m0dii.modules.worldedit;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

interface WorldEditBlockTarget {
    boolean apply(ServerWorld world, BlockPos pos);
}
