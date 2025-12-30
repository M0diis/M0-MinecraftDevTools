import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.server.world.ServerWorld

// Get the server world using the player's world registry key
val serverWorld = server.getWorld(player.world.registryKey) as ServerWorld

// Get the block position at the player's feet
val pos = BlockPos.ofFloored(player.x, player.y - 1, player.z)

// Set the block to diamond block
serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState)

"Placed a diamond block at your feet!"