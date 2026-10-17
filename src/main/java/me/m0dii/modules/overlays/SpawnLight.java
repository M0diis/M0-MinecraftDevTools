package me.m0dii.modules.overlays;

/**
 * Light rules for hostile mob spawning (Java Edition 1.18+): monsters need block light 0. With sky light 7 or
 * less they can spawn at any time; with more sky light only when the sky darkens at night.
 */
public enum SpawnLight {
    SAFE,
    NIGHT,
    ALWAYS;

    public static SpawnLight classify(int blockLight, int skyLight) {
        if (blockLight > 0) {
            return SAFE;
        }
        return skyLight <= 7 ? ALWAYS : NIGHT;
    }
}
