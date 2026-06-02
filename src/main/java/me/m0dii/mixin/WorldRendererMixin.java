package me.m0dii.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WorldRenderer.class, priority = 1001)
public class WorldRendererMixin {

    @Shadow
    private int cameraChunkX;
    @Shadow
    private int cameraChunkZ;

    private int lastUpdatePosX;
    private int lastUpdatePosZ;

}