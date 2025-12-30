package me.m0dii.nbteditor.multiversion;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public interface MVPacketByteBufParent {

    default PacketByteBuf m0_dev_tools$writeBoolean(boolean value) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeBoolean");
    }

    default PacketByteBuf m0_dev_tools$writeDouble(double value) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeDouble");
    }

    default Identifier m0_dev_tools$readIdentifier() {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#readIdentifier");
    }

    default PacketByteBuf m0_dev_tools$writeIdentifier(Identifier id) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeIdentifier");
    }

    default <T> RegistryKey<T> m0_dev_tools$readRegistryKey(RegistryKey<? extends Registry<T>> registryRef) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#readRegistryKey");
    }

    default void m0_dev_tools$writeRegistryKey(RegistryKey<?> key) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeRegistryKey");
    }

    default PacketByteBuf m0_dev_tools$writeNbtCompound(NbtCompound element) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeNbtCompound");
    }

    default Vec3d m0_dev_tools$readVec3d() {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#readVec3d");
    }

    default void m0_dev_tools$writeVec3d(Vec3d vector) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeVec3d");
    }

    default ItemStack m0_dev_tools$readItemStack() {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#readItemStack");
    }

    default PacketByteBuf m0_dev_tools$writeItemStack(ItemStack item) {
        throw new RuntimeException("Missing implementation for MVPacketByteBufParent#writeItemStack");
    }

}
