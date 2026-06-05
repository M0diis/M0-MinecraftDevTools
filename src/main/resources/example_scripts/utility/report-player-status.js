const Text = Java.type('net.minecraft.text.Text');
const StringCls = Java.type('java.lang.String');

if (player == null || world == null) {
    "This example requires a loaded player and world.";
} else {
    const message = StringCls.format(
        'Player %s is at %.1f, %.1f, %.1f in %s with %.1f health and %d food.',
        player.name.string,
        player.x,
        player.y,
        player.z,
        String(world.registryKey.value),
        player.health,
        player.hungerManager.foodLevel
    );

    player.sendMessage(Text.literal(message), false);
    message;
}
