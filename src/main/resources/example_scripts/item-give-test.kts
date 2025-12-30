import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.enchantment.Enchantments

// Helper to get RegistryEntry from RegistryKey
fun getEntry(key: net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment>) =
    Registries.enchantment.getEntry(key).get()

// Give a diamond sword with Sharpness V and Unbreaking III
val sword = ItemStack(Items.DIAMOND_SWORD)
sword.addEnchantment(getEntry(Enchantments.SHARPNESS), 5)
sword.addEnchantment(getEntry(Enchantments.UNBREAKING), 3)
player.giveItemStack(sword)

// Give a stack of enchanted golden apples
val apples = ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 16)
player.giveItemStack(apples)

// Give a bow with Power V and Infinity
val bow = ItemStack(Items.BOW)
bow.addEnchantment(getEntry(Enchantments.POWER), 5)
bow.addEnchantment(getEntry(Enchantments.INFINITY), 1)
player.giveItemStack(bow)

"Gave you a sword, apples, and a bow with enchants!"