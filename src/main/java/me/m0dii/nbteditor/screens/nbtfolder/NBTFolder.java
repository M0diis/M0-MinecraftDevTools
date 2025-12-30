package me.m0dii.nbteditor.screens.nbtfolder;

import me.m0dii.nbteditor.screens.NBTEditorScreen;
import me.m0dii.nbteditor.screens.NBTValue;
import me.m0dii.nbteditor.util.ClassMap;
import net.minecraft.nbt.AbstractNbtList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface NBTFolder<T extends NbtElement> {

    ClassMap<NbtElement, Constructor<?>> TYPES = getTypesMap();

    private static ClassMap<NbtElement, Constructor<?>> getTypesMap() {
        ClassMap<NbtElement, Constructor<?>> output = new ClassMap<>();
        output.put(AbstractNbtList.class, (Constructor<AbstractNbtList<?>>) ListNBTFolder::new);
        output.put(NbtCompound.class, (Constructor<NbtCompound>) CompoundNBTFolder::new);
        output.put(NbtString.class, (Constructor<NbtString>) StringNBTFolder::new);
        return output;
    }

    @SuppressWarnings("unchecked")
    static <T extends NbtElement> NBTFolder<T> get(Class<T> nbt, Supplier<T> get, Consumer<T> set) {
        Constructor<?> constructor = TYPES.get(nbt);
        if (constructor == null) {
            return null;
        }
        return ((Constructor<T>) constructor).create(get, set);
    }

    @SuppressWarnings("unchecked")
    static <T extends NbtElement> NBTFolder<? extends T> get(T nbt) {
        AtomicReference<T> ref = new AtomicReference<>(nbt);
        return get((Class<T>) nbt.getClass(), ref::getPlain, ref::setPlain);
    }

    T getNBT();

    void setNBT(T value);

    List<NBTValue> getEntries(NBTEditorScreen<?> screen);

    boolean hasEmptyKey();

    NbtElement getValue(String key);

    void setValue(String key, NbtElement value);

    void addKey(String key);

    void removeKey(String key);

    Optional<String> getNextKey(Optional<String> pastingKey);

    Predicate<String> getKeyValidator(boolean renaming);

    boolean handlesDuplicateKeys();

    default NBTFolder<?> getSubFolder(String key) {
        NbtElement value = getValue(key);
        if (value == null) {
            return null;
        }
        return getSubFolder(key, value.getClass());
    }

    private <T2 extends NbtElement> NBTFolder<T2> getSubFolder(String key, Class<T2> clazz) {
        return get(clazz, () -> clazz.cast(getValue(key)), newValue -> setValue(key, newValue));
    }

    interface Constructor<T extends NbtElement> {
        NBTFolder<T> create(Supplier<T> get, Consumer<T> set);
    }

}
