package me.m0dii.nbteditor.mixin.toggled;

import me.m0dii.nbteditor.server.ServerMixinLink;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net.minecraft.screen.HorseScreenHandler$2"})
public class HorseScreenHandler2Mixin {
    @Inject(method = "<init>(Lnet/minecraft/class_1724;Lnet/minecraft/class_1263;IIILnet/minecraft/class_1496;)V", at = @At("RETURN"), remap = false)
    @SuppressWarnings("target")
    private void init(HorseScreenHandler handler, Inventory inventory, int index, int x, int y, AbstractHorseEntity horse, CallbackInfo info) {
        PlayerEntity owner = ServerMixinLink.SCREEN_HANDLER_OWNER.get(Thread.currentThread());
        if (owner == null) {
            return;
        }
        ServerMixinLink.SLOT_OWNER.put((Slot) (Object) this, owner);
    }
}
