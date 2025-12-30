package me.m0dii.nbteditor.packets;

import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.MVRegistryKeys;
import me.m0dii.nbteditor.multiversion.networking.ModPacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.UUID;

public class SetEntityC2SPacket implements ModPacket {

    public static final Identifier ID = IdentifierInst.of("m0-dev-tools", "set_entity");

    private final RegistryKey<World> world;
    private final UUID uuid;
    private final Identifier id;
    private final NbtCompound nbt;
    private final boolean recreate;

    public SetEntityC2SPacket(RegistryKey<World> world, UUID uuid, Identifier id, NbtCompound nbt, boolean recreate) {
        this.world = world;
        this.uuid = uuid;
        this.id = id;
        this.nbt = nbt;
        this.recreate = recreate;
    }

    public SetEntityC2SPacket(PacketByteBuf payload) {
        this.world = payload.m0_dev_tools$readRegistryKey(MVRegistryKeys.WORLD);
        this.uuid = payload.readUuid();
        this.id = payload.m0_dev_tools$readIdentifier();
        this.nbt = payload.readNbt();
        this.recreate = payload.readBoolean();
    }

    public RegistryKey<World> getWorld() {
        return world;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Identifier getId() {
        return id;
    }

    public NbtCompound getNbt() {
        return nbt;
    }

    public boolean isRecreate() {
        return recreate;
    }

    @Override
    public void write(PacketByteBuf payload) {
        payload.m0_dev_tools$writeRegistryKey(world);
        payload.writeUuid(uuid);
        payload.m0_dev_tools$writeIdentifier(id);
        payload.m0_dev_tools$writeNbtCompound(nbt);
        payload.m0_dev_tools$writeBoolean(recreate);
    }

    @Override
    public Identifier getPacketId() {
        return ID;
    }

}
