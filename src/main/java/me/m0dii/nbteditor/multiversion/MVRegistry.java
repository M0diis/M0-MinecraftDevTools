package me.m0dii.nbteditor.multiversion;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MVRegistry<T> implements Iterable<T> {

    public static final MVRegistry<Item> ITEM = new MVRegistry<>(Registries.ITEM);
    public static final MVRegistry<Block> BLOCK = new MVRegistry<>(Registries.BLOCK);
    public static final MVRegistry<EntityType<?>> ENTITY_TYPE = new MVRegistry<>(Registries.ENTITY_TYPE);
    public static final MVRegistry<EntityAttribute> ATTRIBUTE = new MVRegistry<>(Registries.ATTRIBUTE);
    public static final MVRegistry<Potion> POTION = new MVRegistry<>(Registries.POTION);
    public static final MVRegistry<StatusEffect> STATUS_EFFECT = new MVRegistry<>(Registries.STATUS_EFFECT);

    private static MVRegistry<Enchantment> ENCHANTMENT;

    private final Registry<T> value;

    private MVRegistry(Registry<T> value) {
        this.value = value;
    }

    public static MVRegistry<Enchantment> getEnchantmentRegistry() {
        Registry<Enchantment> registry = DynamicRegistryManagerHolder.getManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        if (ENCHANTMENT == null || ENCHANTMENT.getInternalValue() != registry) {
            ENCHANTMENT = new MVRegistry<>(registry);
        }
        return ENCHANTMENT;
    }

    public static <V, T extends V> T register(MVRegistry<V> registry, Identifier id, T entry) {
        return Registry.register(registry.getInternalValue(), id, entry);
    }

    private static Identifier getRegistryKeyValue(Object key) {
        return ((RegistryKey<?>) key).getValue();
    }

    public Registry<T> getInternalValue() {
        return value;
    }

    @Override
    public @NotNull Iterator<T> iterator() {
        return (value).iterator();
    }

    public Optional<T> getOrEmpty(Identifier id) {
        return value.getOptionalValue(id);
    }

    public Identifier getId(T entry) {
        return value.getId(entry);
    }

    public T get(Identifier id) {
        return value.get(id);
    }

    public Set<Identifier> getIds() {
        return value.getIds();
    }

    public Set<Map.Entry<Identifier, T>> getEntrySet() {
        Set<Map.Entry<RegistryKey<T>, T>> output = value.getEntrySet();

        return output.stream().map(entry -> Map.entry(getRegistryKeyValue(entry.getKey()), entry.getValue()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean containsId(Identifier id) {
        return value.containsId(id);
    }

}
