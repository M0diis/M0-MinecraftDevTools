package me.m0dii.mixin;

import me.m0dii.modules.placement.PlacementRestrictionModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skips block placements outside the shape chosen in Placement Restriction. */
@Mixin(MultiPlayerGameMode.class)
public class PlacementRestrictionMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void m0dev$restrictPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if (!PlacementRestrictionModule.INSTANCE.allowPlacement(player, hand, hitResult)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
