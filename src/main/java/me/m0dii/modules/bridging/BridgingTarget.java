package me.m0dii.modules.bridging;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record BridgingTarget(BlockPos placePos, Direction supportDirection) {
    public Direction clickSide() {
        return this.supportDirection.getOpposite();
    }

    public BlockPos supportPos() {
        return this.placePos.offset(this.supportDirection);
    }
}
