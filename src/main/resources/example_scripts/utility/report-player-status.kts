if (player == null || world == null) {
    "This example requires a loaded player and world."
} else {
    val message = "Player ${player.name.string} is at %.1f, %.1f, %.1f in %s with %.1f health and %d food."
        .format(
            player.x,
            player.y,
            player.z,
            world.registryKey.value,
            player.health,
            player.hungerManager.foodLevel
        )

    player.sendMessage(net.minecraft.text.Text.literal(message), false)
    message
}
