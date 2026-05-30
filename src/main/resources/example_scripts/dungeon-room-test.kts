import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import kotlin.math.floor

val serverWorld = server.getWorld(world.registryKey) as ServerWorld

val roomWidth = 7
val roomHeight = 5
val roomLength = 7
val numRooms = 8
val doorHeight = 2
val doorWidth = 2

val startX = floor(player.x).toInt()
val startY = floor(player.y).toInt()
val startZ = floor(player.z).toInt()

val wall = Blocks.STONE_BRICKS.defaultState
val floor = Blocks.POLISHED_ANDESITE.defaultState
val air = Blocks.AIR.defaultState
val torch = Blocks.TORCH.defaultState

data class Room(val gx: Int, val gz: Int)

val rooms = mutableListOf(Room(0, 0))
val used = mutableSetOf(Room(0, 0))

val dirs = listOf(
    Room(1, 0),
    Room(-1, 0),
    Room(0, 1),
    Room(0, -1)
)

// Generate random connected room layout
while (rooms.size < numRooms) {
    val base = rooms.random()
    val dir = dirs.random()
    val next = Room(base.gx + dir.gx, base.gz + dir.gz)

    if (next !in used) {
        rooms.add(next)
        used.add(next)
    }
}

fun roomBase(room: Room): Pair<Int, Int> {
    val spacingX = roomWidth - 1
    val spacingZ = roomLength - 1
    return Pair(
        startX + room.gx * spacingX,
        startZ + room.gz * spacingZ
    )
}

fun buildRoom(room: Room) {
    val (baseX, baseZ) = roomBase(room)

    for (y in 0 until roomHeight) {
        for (x in 0 until roomWidth) {
            for (z in 0 until roomLength) {
                val pos = BlockPos(baseX + x, startY + y, baseZ + z)

                val isFloor = y == 0
                val isCeiling = y == roomHeight - 1
                val isWall =
                    x == 0 || x == roomWidth - 1 ||
                            z == 0 || z == roomLength - 1

                when {
                    isFloor -> serverWorld.setBlockState(pos, floor)
                    isCeiling || isWall -> serverWorld.setBlockState(pos, wall)
                    else -> serverWorld.setBlockState(pos, air)
                }
            }
        }
    }

    serverWorld.setBlockState(
        BlockPos(baseX + roomWidth / 2, startY + 1, baseZ + roomLength / 2),
        torch
    )
}

fun carveDoorBetween(a: Room, b: Room) {
    val (ax, az) = roomBase(a)
    val (bx, bz) = roomBase(b)

    val dx = b.gx - a.gx
    val dz = b.gz - a.gz

    if (dx == 1) {
        val doorX = ax + roomWidth - 1
        val doorZ = az + roomLength / 2 - doorWidth / 2
        for (w in 0 until doorWidth) {
            for (y in 1..doorHeight) {
                serverWorld.setBlockState(BlockPos(doorX, startY + y, doorZ + w), air)
                serverWorld.setBlockState(BlockPos(doorX + 1, startY + y, doorZ + w), air)
            }
        }
    }

    if (dx == -1) {
        val doorX = ax
        val doorZ = az + roomLength / 2 - doorWidth / 2
        for (w in 0 until doorWidth) {
            for (y in 1..doorHeight) {
                serverWorld.setBlockState(BlockPos(doorX, startY + y, doorZ + w), air)
                serverWorld.setBlockState(BlockPos(doorX - 1, startY + y, doorZ + w), air)
            }
        }
    }

    if (dz == 1) {
        val doorZ = az + roomLength - 1
        val doorX = ax + roomWidth / 2 - doorWidth / 2
        for (w in 0 until doorWidth) {
            for (y in 1..doorHeight) {
                serverWorld.setBlockState(BlockPos(doorX + w, startY + y, doorZ), air)
                serverWorld.setBlockState(BlockPos(doorX + w, startY + y, doorZ + 1), air)
            }
        }
    }

    if (dz == -1) {
        val doorZ = az
        val doorX = ax + roomWidth / 2 - doorWidth / 2
        for (w in 0 until doorWidth) {
            for (y in 1..doorHeight) {
                serverWorld.setBlockState(BlockPos(doorX + w, startY + y, doorZ), air)
                serverWorld.setBlockState(BlockPos(doorX + w, startY + y, doorZ - 1), air)
            }
        }
    }
}

for (room in rooms) {
    buildRoom(room)
}

for (i in 1 until rooms.size) {
    carveDoorBetween(rooms[i - 1], rooms[i])
}

"Generated $numRooms randomly connected dungeon rooms!"