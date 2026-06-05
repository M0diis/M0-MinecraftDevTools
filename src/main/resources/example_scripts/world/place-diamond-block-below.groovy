import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

if (player == null || world == null || server == null) {
    return "This example requires a loaded server world and player."
}

ServerWorld serverWorld = server.getWorld(world.registryKey)
if (serverWorld == null) {
    return "Couldn't resolve the matching server world."
}

def pos = BlockPos.ofFloored(player.x, player.y - 1.0, player.z)
serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState)

"Placed a diamond block below the player."
