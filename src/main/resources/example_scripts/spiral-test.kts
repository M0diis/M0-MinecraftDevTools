import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import kotlin.math.cos
import kotlin.math.sin

val serverWorld = server.getWorld(world.registryKey) as ServerWorld

val startX = player.x
val startY = player.y - 1.0
val startZ = player.z

val turns = 3
val blocksPerTurn = 16
val heightPerTurn = 3.0
val radiusStep = 0.5

val totalBlocks = turns * blocksPerTurn

for (i in 0 until totalBlocks) {
    val angle = Math.toRadians(i * (360.0 / blocksPerTurn))
    val radius = i * radiusStep / blocksPerTurn

    val x = startX + cos(angle) * (1.5 + radius)
    val y = startY + (i * heightPerTurn / blocksPerTurn)
    val z = startZ + sin(angle) * (1.5 + radius)

    val pos = BlockPos.ofFloored(x, y, z)
    serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState)
}

"Placed"