package me.m0dii.nbteditor.multiversion.mixin;

import io.netty.buffer.ByteBuf;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.multiversion.IdentifierInst;
import me.m0dii.nbteditor.multiversion.MVPacketByteBufParent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PacketByteBuf.class)
public abstract class PacketByteBufMixin implements MVPacketByteBufParent {

    @Final
    @Shadow
    private ByteBuf parent;

    @Shadow
    public abstract String readString();

    @Shadow
    public abstract PacketByteBuf writeString(String str);

    @Shadow
    public abstract double readDouble();

    @Override
    public PacketByteBuf m0_dev_tools$writeBoolean(boolean value) {
        parent.writeBoolean(value);
        return (PacketByteBuf) (Object) this;
    }

    @Override
    public PacketByteBuf m0_dev_tools$writeDouble(double value) {
        parent.writeDouble(value);
        return (PacketByteBuf) (Object) this;
    }

    @Override
    public Identifier m0_dev_tools$readIdentifier() {
        return IdentifierInst.of(readString());
    }

    @Override
    public PacketByteBuf m0_dev_tools$writeIdentifier(Identifier id) {
        return writeString(id.toString());
    }

    @Override
    public <T> RegistryKey<T> m0_dev_tools$readRegistryKey(RegistryKey<? extends Registry<T>> registryRef) {
        return RegistryKey.of(registryRef, m0_dev_tools$readIdentifier());
    }

    @Override
    public void m0_dev_tools$writeRegistryKey(RegistryKey<?> key) {
        m0_dev_tools$writeIdentifier(key.getValue());
    }

    @Override
    public PacketByteBuf m0_dev_tools$writeNbtCompound(NbtCompound element) {
        return ((PacketByteBuf) (Object) this).writeNbt(element);
    }

    @Override
    public Vec3d m0_dev_tools$readVec3d() {
        return new Vec3d(readDouble(), readDouble(), readDouble());
    }

    @Override
    public void m0_dev_tools$writeVec3d(Vec3d vector) {
        m0_dev_tools$writeDouble(vector.getX());
        m0_dev_tools$writeDouble(vector.getY());
        m0_dev_tools$writeDouble(vector.getZ());
    }

    @Override
    public ItemStack m0_dev_tools$readItemStack() {
        return ItemStack.OPTIONAL_PACKET_CODEC.decode(createRegistryByteBuf());
    }

    @Override
    public PacketByteBuf m0_dev_tools$writeItemStack(ItemStack item) {
        ItemStack.OPTIONAL_PACKET_CODEC.encode(createRegistryByteBuf(), item);
        return (PacketByteBuf) (Object) this;
    }

    @Unique
    private RegistryByteBuf createRegistryByteBuf() {
        return new RegistryByteBuf(
                parent,
                (DynamicRegistryManager) DynamicRegistryManagerHolder.get()
        );
    }

}
