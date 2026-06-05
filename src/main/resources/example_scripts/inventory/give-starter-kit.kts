import net.minecraft.item.ItemStack
import net.minecraft.item.Items

if (player == null) {
    "This example requires a player."
} else {
    player.giveItemStack(ItemStack(Items.DIAMOND_SWORD))
    player.giveItemStack(ItemStack(Items.BREAD, 16))
    player.giveItemStack(ItemStack(Items.TORCH, 32))
    player.giveItemStack(ItemStack(Items.COOKED_BEEF, 16))
    "Gave the player a small starter kit."
}
