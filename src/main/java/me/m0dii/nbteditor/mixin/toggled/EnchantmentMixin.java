package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.nbteditor.screens.ConfigScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {
    @Inject(method = "getName(Lnet/minecraft/registry/entry/RegistryEntry;I)Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private static void getName(RegistryEntry<Enchantment> enchantment, int level, CallbackInfoReturnable<Text> info) {
        info.setReturnValue(ConfigScreen.getEnchantNameWithMax(enchantment.value(), level));
    }
}
