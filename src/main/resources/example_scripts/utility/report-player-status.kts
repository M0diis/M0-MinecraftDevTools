import net.minecraft.network.chat.Component

if (player == null || world == null) {
    "This example requires a loaded player and world."
} else {
    val message = "Player ${player.getName().getString()} is at %.1f, %.1f, %.1f in %s with %.1f health and %d food."
        .format(
            player.getX(),
            player.getY(),
            player.getZ(),
            world.dimension().identifier(),
            player.getHealth(),
            player.getFoodData().getFoodLevel()
        )

    player.sendSystemMessage(Component.literal(message))
    message
}
