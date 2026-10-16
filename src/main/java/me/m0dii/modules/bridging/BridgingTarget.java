package me.m0dii.modules.bridging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record BridgingTarget(BlockPos placePos, Direction supportDirection) {
    public Direction clickSide() {
        return this.supportDirection.getOpposite();
    }

    public BlockPos supportPos() {
        return this.placePos.relative(this.supportDirection);
    }
}
