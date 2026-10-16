const Blocks = Java.type('net.minecraft.world.level.block.Blocks');
const BlockPos = Java.type('net.minecraft.core.BlockPos');

if (player == null || world == null || server == null) {
    "This example requires a loaded server world and player.";
} else {
    const dimension = world.dimension();
    const pos = BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ());

    // World edits must run on the server thread.
    server.execute(() => {
        const serverLevel = server.getLevel(dimension);
        if (serverLevel != null) {
            serverLevel.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
        }
    });
    "Placed a diamond block below the player.";
}
