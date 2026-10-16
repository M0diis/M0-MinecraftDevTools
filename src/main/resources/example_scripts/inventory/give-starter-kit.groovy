import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

if (player == null) {
    return "This example requires a player."
}

player.addItem(new ItemStack(Items.DIAMOND_SWORD))
player.addItem(new ItemStack(Items.BREAD, 16))
player.addItem(new ItemStack(Items.TORCH, 32))
player.addItem(new ItemStack(Items.COOKED_BEEF, 16))

"Gave the player a small starter kit."
