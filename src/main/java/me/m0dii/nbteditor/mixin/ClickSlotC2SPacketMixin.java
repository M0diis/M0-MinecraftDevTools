package me.m0dii.nbteditor.mixin;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.packets.ClickSlotC2SPacketParent;
import me.m0dii.nbteditor.screens.ConfigScreen;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClickSlotC2SPacket.class)
public class ClickSlotC2SPacketMixin implements ClickSlotC2SPacketParent {
    private static final int NO_SLOT_RESTRICTIONS_FLAG = 0b01000000;

    @Shadow
    private int button;

    @ModifyVariable(method = "<init>(IIIILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/item/ItemStack;Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;)V", at = @At("HEAD"), ordinal = 3)
    @Group(name = "<init>", min = 1)
    private static int init_new(int button) {
        if (ConfigScreen.isNoSlotRestrictions() && M0DevToolsClient.SERVER_CONN.isEditingExpanded()) {
            return button | NO_SLOT_RESTRICTIONS_FLAG;
        }
        return button;
    }

    @Inject(method = "getButton", at = @At("RETURN"), cancellable = true)
    private void getButton(CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(info.getReturnValue() & ~NO_SLOT_RESTRICTIONS_FLAG);
    }

    @Override
    public boolean isNoSlotRestrictions() {
        return (button & NO_SLOT_RESTRICTIONS_FLAG) != 0;
    }
}
