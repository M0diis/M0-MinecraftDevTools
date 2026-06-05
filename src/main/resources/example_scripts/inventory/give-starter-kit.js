const ItemStack = Java.type('net.minecraft.item.ItemStack');
const Items = Java.type('net.minecraft.item.Items');

if (player == null) {
    "This example requires a player.";
} else {
    player.giveItemStack(new ItemStack(Items.DIAMOND_SWORD));
    player.giveItemStack(new ItemStack(Items.BREAD, 16));
    player.giveItemStack(new ItemStack(Items.TORCH, 32));
    player.giveItemStack(new ItemStack(Items.COOKED_BEEF, 16));
    "Gave the player a small starter kit.";
}
