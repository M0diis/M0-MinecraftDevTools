package me.m0dii.modules.worldedit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

interface WorldEditBlockTarget {
    boolean apply(ServerLevel world, BlockPos pos);
}
