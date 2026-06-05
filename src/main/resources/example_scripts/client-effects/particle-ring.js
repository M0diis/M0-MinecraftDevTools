const ParticleTypes = Java.type('net.minecraft.particle.ParticleTypes');

let result = 'This example requires a loaded client world and player.';

if (player != null && world != null) {
    const particleCount = 24;
    for (let i = 0; i < particleCount; i++) {
        const angle = (i / particleCount) * (2 * Math.PI);
        const x = player.x + Math.cos(angle) * 1.5;
        const y = player.y + 1.0;
        const z = player.z + Math.sin(angle) * 1.5;
        world.addParticle(
            ParticleTypes.HAPPY_VILLAGER,
            x, y, z,
            0.0, 0.03, 0.0
        );
    }
    result = 'Spawned a particle ring around the player.';
}

result;
