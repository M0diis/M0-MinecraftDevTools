import me.m0dii.nbteditor.multiversion.MVRegistry
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking
import me.m0dii.nbteditor.packets.SetBlockC2SPacket
import me.m0dii.nbteditor.util.BlockStateProperties
import me.m0dii.nbteditor.util.MiscUtil
import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

val serverWorld = server.getWorld(player.world.registryKey) as ServerWorld
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
    serverWorld.setBlockState(pos, Blocks.DIAMOND_BLOCK.defaultState)

    val blockState = Blocks.DIAMOND_BLOCK.defaultState
    val blockEntity = serverWorld.getBlockEntity(pos)
    val nbt = if (blockEntity != null) NBTManagers.BLOCK_ENTITY.getOrCreateNbt(blockEntity) else null

    ClientNetworking.send(
        SetBlockC2SPacket(
            MiscUtil.client.world!!.getRegistryKey(),
            pos,
            MVRegistry.BLOCK.getId(blockState.getBlock()),
            BlockStateProperties(blockState),
            nbt,
            true,
            true
        )
    )
}

"Placed"