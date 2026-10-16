import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

if (player == null) {
    "This example requires a player."
} else {
    player.addItem(ItemStack(Items.DIAMOND_SWORD))
    player.addItem(ItemStack(Items.BREAD, 16))
    player.addItem(ItemStack(Items.TORCH, 32))
    player.addItem(ItemStack(Items.COOKED_BEEF, 16))
    "Gave the player a small starter kit."
}
