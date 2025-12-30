package me.m0dii.nbteditor.multiversion.nbt;

import net.minecraft.nbt.NbtCompound;

public interface DeserializableNBTManager<T> extends NBTManager<T> {
    Attempt<T> tryDeserialize(NbtCompound nbt);

    default T deserialize(NbtCompound nbt, boolean requireSuccess) {
        Attempt<T> attempt = tryDeserialize(nbt);
        return requireSuccess ? attempt.getSuccessOrThrow() : attempt.getAttemptOrThrow();
    }
}
