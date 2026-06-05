if (player == null || world == null) {
    return "This example requires a loaded player and world."
}

def message = String.format(
    "Player %s is at %.1f, %.1f, %.1f in %s with %.1f health and %d food.",
    player.name?.string,
    player.x,
    player.y,
    player.z,
    world.registryKey.value,
    player.health,
    player.hungerManager.foodLevel
)

player.sendMessage(net.minecraft.text.Text.literal(message), false)
message
