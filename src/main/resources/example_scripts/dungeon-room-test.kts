import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.server.world.ServerWorld

val serverWorld = server.getWorld(player.world.registryKey) as ServerWorld

val roomWidth = 7
val roomHeight = 5
val roomLength = 7
val numRooms = 3
val doorHeight = 2
val doorWidth = 2

val startX = player.x.toInt()
val startY = player.y.toInt()
val startZ = player.z.toInt()

for (room in 0 until numRooms) {
    val baseX = startX + room * roomWidth // No gap between rooms
    // Build floor, ceiling, and walls
    for (y in 0 until roomHeight) {
        for (x in 0 until roomWidth) {
            for (z in 0 until roomLength) {
                val wx = baseX + x
                val wy = startY + y
                val wz = startZ + z
                val isWall = x == 0 || x == roomWidth - 1 || z == 0 || z == roomLength - 1 || y == 0 || y == roomHeight - 1
                val pos = BlockPos(wx, wy, wz)
                if (isWall) {
                    serverWorld.setBlockState(pos, Blocks.STONE_BRICKS.defaultState)
                } else {
                    serverWorld.setBlockState(pos, Blocks.AIR.defaultState)
                }
            }
        }
    }
    // Carve door to next room (except last room)
    if (room < numRooms - 1) {
        val doorX = baseX + roomWidth - 1
        val doorZStart = startZ + roomLength / 2 - doorWidth / 2
        for (dz in 0 until doorWidth) {
            for (dy in 1..doorHeight) {
                val pos = BlockPos(doorX, startY + dy, doorZStart + dz)
                serverWorld.setBlockState(pos, Blocks.AIR.defaultState)
            }
        }
        // Carve corridor between this room and the next
        val corridorStartX = doorX + 1
        val corridorEndX = doorX + 2 // corridor length = 1 block (since rooms are adjacent)
        for (cx in corridorStartX..corridorEndX) {
            for (dz in 0 until doorWidth) {
                for (dy in 1..doorHeight) {
                    val pos = BlockPos(cx, startY + dy, doorZStart + dz)
                    serverWorld.setBlockState(pos, Blocks.AIR.defaultState)
                }
            }
        }
    }
    // Carve door to previous room (except first room)
    if (room > 0) {
        val doorX = baseX
        val doorZStart = startZ + roomLength / 2 - doorWidth / 2
        for (dz in 0 until doorWidth) {
            for (dy in 1..doorHeight) {
                val pos = BlockPos(doorX, startY + dy, doorZStart + dz)
                serverWorld.setBlockState(pos, Blocks.AIR.defaultState)
            }
        }
    }
}

"Generated $numRooms connected dungeon rooms with corridors!"
