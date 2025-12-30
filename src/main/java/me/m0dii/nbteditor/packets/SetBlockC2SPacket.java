package me.m0dii.nbteditor.packets;

import lombok.Getter;
import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.MVRegistryKeys;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import me.m0dii.nbteditor.util.BlockStateProperties;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Getter
public class SetBlockC2SPacket implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "set_block");

    private final RegistryKey<World> world;
    private final BlockPos pos;
    private final Identifier id;
    private final BlockStateProperties state;
    private final NbtCompound nbt;
    private final boolean recreate;
    private final boolean triggerUpdate;

    public SetBlockC2SPacket(RegistryKey<World> world,
                             BlockPos pos,
                             Identifier id,
                             BlockStateProperties state,
                             NbtCompound nbt,
                             boolean recreate,
                             boolean triggerUpdate) {
        this.world = world;
        this.pos = pos;
        this.id = id;
        this.state = state;
        this.nbt = nbt;
        this.recreate = recreate;
        this.triggerUpdate = triggerUpdate;
    }

    public SetBlockC2SPacket(PacketByteBuf payload) {
        this.world = payload.m0_dev_tools$readRegistryKey(MVRegistryKeys.WORLD);
        this.pos = payload.readBlockPos();
        this.id = payload.m0_dev_tools$readIdentifier();
        this.state = new BlockStateProperties(payload);
        this.nbt = payload.readNbt();
        this.recreate = payload.readBoolean();
        this.triggerUpdate = payload.readBoolean();
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.m0_dev_tools$writeRegistryKey(world);
        payload.writeBlockPos(pos);
        payload.m0_dev_tools$writeIdentifier(id);
        state.writeToPayload(payload);
        payload.m0_dev_tools$writeNbtCompound(nbt);
        payload.m0_dev_tools$writeBoolean(recreate);
        payload.m0_dev_tools$writeBoolean(triggerUpdate);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
