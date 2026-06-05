import net.minecraft.particle.ParticleTypes

if (player == null || world == null) {
    return "This example requires a loaded client world and player."
}

def particleManager = client?.particleManager
if (particleManager == null) {
    return "Particle manager is unavailable."
}

int particleCount = 24
for (int i = 0; i < particleCount; i++) {
    double angle = Math.toRadians(i * (360.0 / particleCount))
    double x = player.x + Math.cos(angle) * 1.5
    double y = player.y + 1.0
    double z = player.z + Math.sin(angle) * 1.5

    particleManager.addParticle(
            ParticleTypes.HAPPY_VILLAGER,
            x, y, z,
            0.0, 0.03, 0.0
    )
}

"Spawned a particle ring around the player."
