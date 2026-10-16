import net.minecraft.core.particles.ParticleTypes
import kotlin.math.cos
import kotlin.math.sin

if (player == null || world == null) {
    "This example requires a loaded client world and player."
} else {
    val particleCount = 24
    for (i in 0 until particleCount) {
        val angle = Math.toRadians(i * (360.0 / particleCount))
        val x = player.getX() + cos(angle) * 1.5
        val y = player.getY() + 1.0
        val z = player.getZ() + sin(angle) * 1.5

        world.addParticle(
            ParticleTypes.HAPPY_VILLAGER,
            x, y, z,
            0.0, 0.03, 0.0
        )
    }
    "Spawned a particle ring around the player."
}
