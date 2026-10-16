import net.minecraft.network.chat.Component

if (player == null || world == null) {
    return "This example requires a loaded player and world."
}

def message = String.format(
    "Player %s is at %.1f, %.1f, %.1f in %s with %.1f health and %d food.",
    player.getName().getString(),
    player.getX(),
    player.getY(),
    player.getZ(),
    world.dimension().identifier(),
    player.getHealth(),
    player.getFoodData().getFoodLevel()
)

player.sendSystemMessage(Component.literal(message))
message
