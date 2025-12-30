package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.nbteditor.multiversion.DynamicRegistryManagerHolder;
import me.m0dii.nbteditor.multiversion.RegistryCache;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.registry.Registry$1")
public class Registry1Mixin {

    @ModifyVariable(method = "getRawId", at = @At("HEAD"))
    private RegistryEntry<?> getRawId(RegistryEntry<?> entry) {
        if (entry instanceof RegistryEntry.Reference<?> ref && DynamicRegistryManagerHolder.isOwnedByDefaultManager(ref)) {
            RegistryEntry.Reference<?> convertedRef = RegistryCache.convertManagerWithCache(ref);
            if (convertedRef != null) {
                return convertedRef;
            }
        }

        return entry;
    }

}
