package me.m0dii.modules.overlays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnLightTest {
    @Test
    void anyBlockLightMakesItSafe() {
        assertEquals(SpawnLight.SAFE, SpawnLight.classify(1, 0));
        assertEquals(SpawnLight.SAFE, SpawnLight.classify(14, 15));
    }

    @Test
    void darkBlocksSpawnAlwaysOrAtNight() {
        assertEquals(SpawnLight.ALWAYS, SpawnLight.classify(0, 0));
        assertEquals(SpawnLight.ALWAYS, SpawnLight.classify(0, 7));
        assertEquals(SpawnLight.NIGHT, SpawnLight.classify(0, 8));
        assertEquals(SpawnLight.NIGHT, SpawnLight.classify(0, 15));
    }
}
