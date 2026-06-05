const Blocks = Java.type('net.minecraft.block.Blocks');
const BlockPos = Java.type('net.minecraft.util.math.BlockPos');

if (player == null || world == null || server == null) {
    "This example requires a loaded server world and player.";
} else {
    const serverWorld = server.getWorld(world.registryKey);
    if (serverWorld == null) {
        "Couldn't resolve the matching server world.";
    } else {
        const pos = BlockPos.ofFloored(player.x, player.y - 1.0, player.z);
        serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState);
        "Placed a diamond block below the player.";
    }
}
