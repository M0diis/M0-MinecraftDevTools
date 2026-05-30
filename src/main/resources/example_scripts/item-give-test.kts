import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys

fun ench(key: RegistryKey<Enchantment>) =
    player.getEntityWorld().getRegistryManager()
        .getOrThrow(RegistryKeys.ENCHANTMENT)
        .getEntry(key.value)
        .orElseThrow()

val sword = ItemStack(Items.DIAMOND_SWORD)
sword.addEnchantment(ench(Enchantments.SHARPNESS), 5)
sword.addEnchantment(ench(Enchantments.UNBREAKING), 3)
player.giveItemStack(sword)

val apples = ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 16)
player.giveItemStack(apples)

val bow = ItemStack(Items.BOW)
bow.addEnchantment(ench(Enchantments.POWER), 5)
bow.addEnchantment(ench(Enchantments.INFINITY), 1)
player.giveItemStack(bow)

"Gave you a sword, apples, and a bow with enchants!"