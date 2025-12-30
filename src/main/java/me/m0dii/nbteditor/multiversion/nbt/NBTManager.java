package me.m0dii.nbteditor.multiversion.nbt;

import net.minecraft.nbt.NbtCompound;

/**
 * The NBT returned from the methods is a copy and the NBT passed into the methods will be copied
 */
public interface NBTManager<T> {
    Attempt<NbtCompound> trySerialize(T subject);

    default NbtCompound serialize(T subject, boolean requireSuccess) {
        Attempt<NbtCompound> attempt = trySerialize(subject);
        return requireSuccess ? attempt.getSuccessOrThrow() : attempt.getAttemptOrThrow();
    }

    /**
     * Note: If this returns false, {@link #getNbt(T)} may still return an empty {@link NbtCompound} rather than null!
     */
    boolean hasNbt(T subject);

    NbtCompound getNbt(T subject);

    NbtCompound getOrCreateNbt(T subject);

    void setNbt(T subject, NbtCompound nbt);

    default String getNbtString(T subject) {
        NbtCompound nbt = getNbt(subject);
        if (nbt == null) {
            return "";
        }
        return nbt.asString();
    }
}
