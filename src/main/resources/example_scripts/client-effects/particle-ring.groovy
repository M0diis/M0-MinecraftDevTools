import net.minecraft.core.particles.ParticleTypes

if (player == null || world == null) {
    return "This example requires a loaded client world and player."
}

int particleCount = 24
for (int i = 0; i < particleCount; i++) {
    double angle = Math.toRadians(i * (360.0 / particleCount))
    double x = player.getX() + Math.cos(angle) * 1.5
    double y = player.getY() + 1.0
    double z = player.getZ() + Math.sin(angle) * 1.5

    world.addParticle(
            ParticleTypes.HAPPY_VILLAGER,
            x, y, z,
            0.0, 0.03, 0.0
    )
}

"Spawned a particle ring around the player."
