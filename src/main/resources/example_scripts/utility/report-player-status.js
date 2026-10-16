const Component = Java.type('net.minecraft.network.chat.Component');
const StringCls = Java.type('java.lang.String');

if (player == null || world == null) {
    "This example requires a loaded player and world.";
} else {
    const message = StringCls.format(
        'Player %s is at %.1f, %.1f, %.1f in %s with %.1f health and %d food.',
        player.getName().getString(),
        player.getX(),
        player.getY(),
        player.getZ(),
        String(world.dimension().identifier()),
        player.getHealth(),
        player.getFoodData().getFoodLevel()
    );

    player.sendSystemMessage(Component.literal(message));
    message;
}
