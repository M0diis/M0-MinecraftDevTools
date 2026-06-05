import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

if (player == null || world == null || server == null) {
    "This example requires a loaded server world and player."
} else {
    val serverWorld = server.getWorld(world.registryKey) as? ServerWorld
    if (serverWorld == null) {
        "Couldn't resolve the matching server world."
    } else {
        val pos = BlockPos.ofFloored(player.x, player.y - 1.0, player.z)
        serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState)
        "Placed a diamond block below the player."
    }
}
