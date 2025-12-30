package me.m0dii.nbteditor.multiversion;

import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.tagreferences.specific.data.Enchants;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

public class MVEnchantments {

    public static final Enchantment FIRE_ASPECT = getEnchantment();

    private static Enchantment getEnchantment() {
        return MVRegistry.getEnchantmentRegistry().get((Enchantments.FIRE_ASPECT).getValue());
    }

    public static boolean isCursed(Enchantment enchant) {
        return MVRegistry.getEnchantmentRegistry().getInternalValue().getEntry(enchant).isIn(EnchantmentTags.CURSE);
    }

    public static void addEnchantment(ItemStack item, Enchantment enchant, int level) {
        Enchants enchants = ItemTagReferences.ENCHANTMENTS.get(item);
        enchants.addEnchant(enchant, level);
        ItemTagReferences.ENCHANTMENTS.set(item, enchants);
    }

    public static Text getEnchantmentName(Enchantment enchant) {
        Formatting color = (isCursed(enchant) ? Formatting.RED : Formatting.GRAY);
        MutableText output = enchant.description().copy();
        Texts.setStyleIfAbsent(output, Style.EMPTY.withColor(color));
        return output;
    }

}
