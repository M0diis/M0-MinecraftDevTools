import net.minecraft.particle.ParticleTypes
import kotlin.math.cos
import kotlin.math.sin

val particleManager = client.particleManager

for (i in 0 until 20) {
    val angle = Math.toRadians(i * (360.0 / 20))
    val x = player.x + cos(angle) * 1.5
    val y = player.y + 1.0
    val z = player.z + sin(angle) * 1.5

    particleManager.addParticle(
        ParticleTypes.HAPPY_VILLAGER,
        x, y, z,
        0.0, 0.1, 0.0
    )
}

"Spawned"