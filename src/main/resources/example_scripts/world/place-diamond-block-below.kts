import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks

if (player == null || world == null || server == null) {
    "This example requires a loaded server world and player."
} else {
    val dimension = world.dimension()
    val pos = BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ())

    // World edits must run on the server thread.
    server.execute {
        server.getLevel(dimension)?.setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState())
    }
    "Placed a diamond block below the player."
}
