import net.minecraft.particle.ParticleTypes

for (i in 0 until 20) {
    val angle = Math.toRadians(i * (360.0 / 20))
    val x = player.x + Math.cos(angle) * 1.5
    val y = player.y + 1.0
    val z = player.z + Math.sin(angle) * 1.5
    world.addParticle(ParticleTypes.HAPPY_VILLAGER, x, y, z, 0.0, 0.1, 0.0)
}

"Spawned"