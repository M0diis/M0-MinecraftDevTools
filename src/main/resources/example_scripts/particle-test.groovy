import net.minecraft.particle.ParticleTypes

def player = source
if (!(player instanceof net.minecraft.client.network.ClientPlayerEntity)) {
    return "Invalid player object: ${player}. Expected a ClientPlayerEntity.";
}

def world = player.world
if (world == null) {
    return "This script requires a client-side world to display particles.";
}

def center = player.getPos()
if (center == null) {
    return "Couldn't resolve player position.";
}

// Generate a dynamic particle sphere
int particleCount = 500
float maxRadius = 5.0f
float speed = 0.1f
float angleStep = (float) (2 * Math.PI / particleCount)

for (int i = 0; i < particleCount; i++) {
    float angle = i * angleStep
    double x = center.x + maxRadius * Math.cos(angle) * Math.sin(i * speed)
    double y = center.y + maxRadius * Math.sin(angle) * Math.sin(i * speed)
    double z = center.z + maxRadius * Math.cos(i * speed)

    world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0)
}

return "Generated a dynamic particle sphere around the player.";
