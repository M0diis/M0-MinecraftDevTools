package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.MVRegistryKeys;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GetBlockC2SPacket implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "get_block");

    private final int requestId;
    private final RegistryKey<World> world;
    private final BlockPos pos;

    public GetBlockC2SPacket(int requestId, RegistryKey<World> world, BlockPos pos) {
        this.requestId = requestId;
        this.world = world;
        this.pos = pos;
    }

    public GetBlockC2SPacket(PacketByteBuf payload) {
        this.requestId = payload.readVarInt();
        this.world = payload.m0_dev_tools$readRegistryKey(MVRegistryKeys.WORLD);
        this.pos = payload.readBlockPos();
    }

    public int getRequestId() {
        return requestId;
    }

    public RegistryKey<World> getWorld() {
        return world;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.writeVarInt(requestId);
        payload.m0_dev_tools$writeRegistryKey(world);
        payload.writeBlockPos(pos);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
