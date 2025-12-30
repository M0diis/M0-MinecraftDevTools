package me.m0dii.nbteditor.tagreferences.specific;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.m0dii.nbteditor.multiversion.MVComponentType;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.tagreferences.general.ComponentTagReference;
import me.m0dii.nbteditor.tagreferences.general.TagReference;
import me.m0dii.nbteditor.tagreferences.specific.data.Enchants;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.stream.Collectors;

public class EnchantsTagReference implements TagReference<Enchants, ItemStack> {

    private static final TagReference<Enchants, ItemStack> ENCHANTMENTS = getEnchantsTagRef(MVComponentType.ENCHANTMENTS);
    private static final TagReference<Enchants, ItemStack> STORED_ENCHANTMENTS = getEnchantsTagRef(MVComponentType.STORED_ENCHANTMENTS);

    public EnchantsTagReference() {

    }

    private static TagReference<Enchants, ItemStack> getEnchantsTagRef(MVComponentType<ItemEnchantmentsComponent> component) {
        return new ComponentTagReference<>(component,
                null,
                componentValue -> componentValue == null ? new Enchants() : new Enchants(componentValue.getEnchantmentEntries().stream()
                        .map(entry -> new Enchants.EnchantWithLevel(entry.getKey().value(), entry.getIntValue())).collect(Collectors.toList())),
                (componentValue, enchants) -> new ItemEnchantmentsComponent(new Object2IntOpenHashMap<>(
                        enchants.enchants().stream().collect(Collectors.toMap(
                                enchant -> MVRegistry.getEnchantmentRegistry().getInternalValue().getEntry(enchant.enchant()),
                                enchant -> Math.min(255, enchant.level()),
                                Math::max))),
                        componentValue == null || componentValue.showInTooltip));
    }

    @Override
    public Enchants get(ItemStack object) {
        return object.isOf(Items.ENCHANTED_BOOK) ? STORED_ENCHANTMENTS.get(object) : ENCHANTMENTS.get(object);
    }

    @Override
    public void set(ItemStack object, Enchants value) {
        if (object.isOf(Items.ENCHANTED_BOOK)) {
            STORED_ENCHANTMENTS.set(object, value);
        } else {
            ENCHANTMENTS.set(object, value);
        }
    }

}
