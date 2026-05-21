package me.m0dii.modules.watson;

import net.minecraft.util.math.BlockPos;

/**
 * One parsed CoreProtect log line with render metadata.
 */
public record CoreProtectEntry(
        BlockPos pos,
        String actor,
        Action action,
        long observedAt,
        String rawMessage
) {
    public enum Action {
        PLACE,
        REMOVE,
        USE,
        LOOKUP,
        UNKNOWN
    }
}

