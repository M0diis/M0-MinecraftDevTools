import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

val startX = player.x
val startY = player.y - 1
val startZ = player.z

val turns = 3
val blocksPerTurn = 16
val heightPerTurn = 3.0
val radiusStep = 0.5

val totalBlocks = turns * blocksPerTurn

for (i in 0 until totalBlocks) {
    val angle = Math.toRadians(i * (360.0 / blocksPerTurn))
    val radius = i * radiusStep / blocksPerTurn
    val x = startX + Math.cos(angle) * (1.5 + radius)
    val y = startY + (i * heightPerTurn / blocksPerTurn)
    val z = startZ + Math.sin(angle) * (1.5 + radius)
    val pos = BlockPos.ofFloored(x, y, z)
    val cmd = "setblock ${pos.x} ${pos.y} ${pos.z} minecraft:diamond_block"
    try {
        player.networkHandler.sendChatCommand(cmd)
    } catch (e: Exception) {
        println("[SpiralTest] Failed to send command for $pos: ${e.message}")
    }
}
"Placed $totalBlocks blocks using /setblock."
