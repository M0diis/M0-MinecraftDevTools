package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.ParallelResourceReload;
import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static ResourceReload init_monitor(ResourceReload monitor) {
        return new ParallelResourceReload(monitor, DynamicRegistryManagerHolder.loadDefaultManager());
    }
}
