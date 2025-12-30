package me.m0dii.nbteditor.multiversion.nbt;

import net.minecraft.nbt.NbtCompound;

import java.util.function.Consumer;

/**
 * Convenience interface to avoid <code>NBTManagers.ITEM.getNbt(item)</code>
 */
public interface IntegratedNBTManager {
    default NbtCompound manager$serialize(boolean requireSuccess) {
        throw new RuntimeException("Missing implementation for IntegratedNBTManager#manager$serialize");
    }

    default boolean manager$hasNbt() {
        throw new RuntimeException("Missing implementation for IntegratedNBTManager#manager$hasNbt");
    }

    default NbtCompound manager$getNbt() {
        throw new RuntimeException("Missing implementation for IntegratedNBTManager#manager$getNbt");
    }

    default NbtCompound manager$getOrCreateNbt() {
        throw new RuntimeException("Missing implementation for IntegratedNBTManager#manager$getOrCreateNbt");
    }

    default void manager$setNbt(NbtCompound nbt) {
        throw new RuntimeException("Missing implementation for IntegratedNBTManager#manager$setNbt");
    }

    default void manager$modifyNbt(Consumer<NbtCompound> modifier) {
        NbtCompound nbt = manager$getOrCreateNbt();
        modifier.accept(nbt);
        manager$setNbt(nbt);
    }

    default void manager$modifySubNbt(String tag, Consumer<NbtCompound> modifier) {
        NbtCompound nbt = manager$getOrCreateNbt();
        NbtCompound subNbt = nbt.getCompound(tag);
        modifier.accept(subNbt);
        nbt.put(tag, subNbt);
        manager$setNbt(nbt);
    }
}
