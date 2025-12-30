package me.m0dii.nbteditor.multiversion.nbt;

import me.m0dii.nbteditor.multiversion.MVComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public interface MVItemStackParent {
    default boolean manager$hasCustomName() {
        throw new RuntimeException("Missing implementation for MVItemStackParent#manager$hasCustomName");
    }

    default ItemStack manager$setCustomName(Text name) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#manager$setCustomName");
    }

    default boolean contains(MVComponentType<?> type) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#contains");
    }

    default <T> T get(MVComponentType<T> type) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#get");
    }

    default <T> T getOrDefault(MVComponentType<T> type, T fallback) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#getOrDefault");
    }

    default <T> T set(MVComponentType<T> type, T value) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#set");
    }

    default <T> T apply(MVComponentType<T> type, T defaultValue, UnaryOperator<T> applier) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#apply");
    }

    default <T, U> T apply(MVComponentType<T> type, T defaultValue, U change, BiFunction<T, U, T> applier) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#apply");
    }

    default <T> T remove(MVComponentType<T> type) {
        throw new RuntimeException("Missing implementation for MVItemStackParent#remove");
    }
}
