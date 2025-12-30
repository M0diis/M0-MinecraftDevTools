package me.m0dii.nbteditor.tagreferences.general;

import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.*;

public interface TagReference<T, O> {
    static <T1, T2, O> TagReference<T2, O> mapValue(Function<T1, T2> getter, Function<T2, T1> setter, TagReference<T1, O> tagRef) {
        return new TagReference<>() {
            @Override
            public T2 get(O object) {
                return getter.apply(tagRef.get(object));
            }

            @Override
            public void set(O object, T2 value) {
                tagRef.set(object, setter.apply(value));
            }
        };
    }

    static <T, O1, O2> TagReference<T, O2> mapObject(Function<O2, O1> getter, BiConsumer<O2, O1> setter, TagReference<T, O1> tagRef) {
        return new TagReference<>() {
            @Override
            public T get(O2 object) {
                return tagRef.get(getter.apply(object));
            }

            @Override
            public void set(O2 object, T value) {
                O1 internalObject = getter.apply(object);
                tagRef.set(internalObject, value);
                setter.accept(object, internalObject);
            }
        };
    }

    static <T> TagReference<T, ItemStack> forItems(Supplier<T> defaultValue, TagReference<T, NbtCompound> tagRef) {
        return new TagReference<>() {
            @Override
            public T get(ItemStack object) {
                if (!NBTManagers.ITEM.hasNbt(object)) {
                    return defaultValue.get();
                }
                return tagRef.get(NBTManagers.ITEM.getNbt(object));
            }

            @Override
            public void set(ItemStack object, T value) {
                NbtCompound itemNbt = NBTManagers.ITEM.getOrCreateNbt(object);
                tagRef.set(itemNbt, value);
            }
        };
    }

    static <T, O extends LocalNBT> TagReference<T, O> forLocalNBT(Supplier<T> defaultValue, TagReference<T, NbtCompound> tagRef) {
        return new TagReference<>() {
            @Override
            public T get(O object) {
                NbtCompound nbt = object.getNBT();
                if (nbt == null) {
                    return defaultValue.get();
                }
                return tagRef.get(nbt);
            }

            @Override
            public void set(O object, T value) {
                object.modifyNBT(nbt -> {
                    tagRef.set(nbt, value);
                });
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <C, O> TagReference<List<C>, O> forLists(Class<C> clazz, TagReference<C[], O> tagRef) {
        return mapValue(
                array -> new ArrayList<>(Arrays.asList(array)),
                list -> list == null ? null : list.toArray(len -> (C[]) Array.newInstance(clazz, len)),
                tagRef);
    }

    static <C, O> TagReference<List<C>, O> forLists(Function<NbtElement, C> getter, Function<C, NbtElement> setter, TagReference<NbtList, O> tagRef) {
        return mapValue(
                nbtList -> {
                    List<C> list = new ArrayList<>();
                    for (NbtElement elementNbt : nbtList) {
                        C elementValue = getter.apply(elementNbt);
                        if (elementValue != null) {
                            list.add(elementValue);
                        }
                    }
                    return list;
                },
                list -> {
                    if (list == null) {
                        return null;
                    }
                    NbtList nbtList = new NbtList();
                    for (C elementValue : list) {
                        NbtElement elementNbt = setter.apply(elementValue);
                        if (elementNbt != null) {
                            nbtList.add(elementNbt);
                        }
                    }
                    return nbtList;
                },
                tagRef);
    }

    static <V, O> TagReference<Map<String, V>, O> forMaps(Function<NbtElement, V> getter, Function<V, NbtElement> setter, TagReference<NbtCompound, O> tagRef) {
        return mapValue(
                compound -> {
                    Map<String, V> output = new HashMap<>();
                    for (String key : compound.getKeys()) {
                        V entryValue = getter.apply(compound.get(key));
                        if (entryValue != null) {
                            output.put(key, entryValue);
                        }
                    }
                    return output;
                },
                map -> {
                    if (map == null) {
                        return null;
                    }
                    NbtCompound compound = new NbtCompound();
                    map.forEach((key, entryValue) -> {
                        NbtElement entryValueNbt = setter.apply(entryValue);
                        if (entryValueNbt != null) {
                            compound.put(key, entryValueNbt);
                        }
                    });
                    return compound;
                },
                tagRef);
    }

    static <T> TagReference<T, NbtCompound> alsoRemove(String path, TagReference<T, NbtCompound> tagRef) {
        return new TagReference<>() {
            @Override
            public T get(NbtCompound object) {
                return tagRef.get(object);
            }

            @Override
            public void set(NbtCompound object, T value) {
                tagRef.set(object, value);

                String[] pathParts = path.split("/");
                NbtCompound nbt = object;
                for (int i = 0; i < pathParts.length - 1; i++)
                    nbt = nbt.getCompound(pathParts[i]);
                nbt.remove(pathParts[pathParts.length - 1]);
            }
        };
    }

    /**
     * If T is a collection, this should return a mutable copy
     */
    T get(O object);

    void set(O object, T value);

    default void modify(O object, UnaryOperator<T> modifier) {
        set(object, modifier.apply(get(object)));
    }

    default void modify(O object, Consumer<T> modifier) {
        modify(object, value -> {
            modifier.accept(value);
            return value;
        });
    }
}
