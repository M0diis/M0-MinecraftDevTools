import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import kotlin.math.floor

val serverWorld = server.getWorld(world.registryKey) as ServerWorld

val roomWidth = 7
val roomHeight = 5
val roomLength = 7
val numRooms = 10
val corridorWidth = 3
val corridorHeight = 3
val spacing = 12

val startX = floor(player.x).toInt()
val startY = floor(player.y).toInt()
val startZ = floor(player.z).toInt()

val wall = Blocks.STONE_BRICKS.defaultState
val floor = Blocks.POLISHED_ANDESITE.defaultState
val air = Blocks.AIR.defaultState
val torch = Blocks.TORCH.defaultState

data class Room(val gx: Int, val gz: Int)

val dirs = listOf(
    Room(1, 0),
    Room(-1, 0),
    Room(0, 1),
    Room(0, -1)
)

val rooms = mutableListOf(Room(0, 0))
val used = mutableSetOf(Room(0, 0))
val links = mutableListOf<Pair<Room, Room>>()

while (rooms.size < numRooms) {
    val from = rooms.random()
    val dir = dirs.random()
    val next = Room(from.gx + dir.gx, from.gz + dir.gz)

    if (next !in used) {
        rooms.add(next)
        used.add(next)
        links.add(from to next)
    }
}

fun base(room: Room): Pair<Int, Int> {
    return Pair(
        startX + room.gx * spacing,
        startZ + room.gz * spacing
    )
}

fun setBox(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int, block: net.minecraft.block.BlockState) {
    for (x in minOf(x1, x2)..maxOf(x1, x2)) {
        for (y in minOf(y1, y2)..maxOf(y1, y2)) {
            for (z in minOf(z1, z2)..maxOf(z1, z2)) {
                serverWorld.setBlockState(BlockPos(x, y, z), block)
            }
        }
    }
}

fun buildRoom(room: Room) {
    val (bx, bz) = base(room)

    for (x in 0 until roomWidth) {
        for (y in 0 until roomHeight) {
            for (z in 0 until roomLength) {
                val isFloor = y == 0
                val isCeiling = y == roomHeight - 1
                val isWall = x == 0 || x == roomWidth - 1 || z == 0 || z == roomLength - 1

                val block = when {
                    isFloor -> floor
                    isCeiling || isWall -> wall
                    else -> air
                }

                serverWorld.setBlockState(BlockPos(bx + x, startY + y, bz + z), block)
            }
        }
    }

    serverWorld.setBlockState(
        BlockPos(bx + roomWidth / 2, startY + 1, bz + roomLength / 2),
        torch
    )
}

fun carveCorridor(a: Room, b: Room) {
    val (ax, az) = base(a)
    val (bx, bz) = base(b)

    val acx = ax + roomWidth / 2
    val acz = az + roomLength / 2
    val bcx = bx + roomWidth / 2
    val bcz = bz + roomLength / 2

    val half = corridorWidth / 2

    if (acx != bcx) {
        setBox(
            acx,
            startY + 1,
            acz - half,
            bcx,
            startY + corridorHeight,
            acz + half,
            air
        )

        setBox(
            acx,
            startY,
            acz - half,
            bcx,
            startY,
            acz + half,
            floor
        )
    }

    if (acz != bcz) {
        setBox(
            bcx - half,
            startY + 1,
            acz,
            bcx + half,
            startY + corridorHeight,
            bcz,
            air
        )

        setBox(
            bcx - half,
            startY,
            acz,
            bcx + half,
            startY,
            bcz,
            floor
        )
    }
}

for (room in rooms) {
    buildRoom(room)
}

for ((a, b) in links) {
    carveCorridor(a, b)
}

"Generated $numRooms connected dungeon rooms with proper corridors!"