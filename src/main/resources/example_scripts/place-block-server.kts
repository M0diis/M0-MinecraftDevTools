import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

val serverWorld = server.getWorld(world.registryKey) as ServerWorld

val pos = BlockPos.ofFloored(
    player.x,
    player.y - 1.0,
    player.z
)

serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState)

"Placed a real diamond block at your feet!"