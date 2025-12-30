package me.m0dii.nbteditor.tagreferences.general;

import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ArraySplitTagReference<C, O> implements TagReference<List<C>, O> {

    private final Supplier<C> entryPadder;
    private final TagReference<C, O>[] entryTagRefs;

    @SafeVarargs
    public ArraySplitTagReference(Supplier<C> entryPadder, TagReference<C, O>... entryTagRefs) {
        this.entryPadder = entryPadder;
        this.entryTagRefs = entryTagRefs;
    }

    @SuppressWarnings("unchecked")
    public static <C> TagReference<List<C>, NbtCompound> forNBT(Supplier<C> entryPadder, Class<C> clazz, String... paths) {
        return new ArraySplitTagReference<>(entryPadder,
                Arrays.stream(paths).map(path -> new NBTTagReference<>(clazz, path)).toArray(TagReference[]::new));
    }

    @Override
    public List<C> get(O object) {
        return Arrays.stream(entryTagRefs).map(tagRef -> tagRef.get(object)).collect(Collectors.toList());
    }

    @Override
    public void set(O object, List<C> value) {
        if (value == null) {
            for (TagReference<C, O> entryTagRef : entryTagRefs)
                entryTagRef.set(object, null);
            return;
        }
        for (int i = 0; i < entryTagRefs.length; i++)
            entryTagRefs[i].set(object, i < value.size() ? value.get(i) : entryPadder.get());
    }

}
