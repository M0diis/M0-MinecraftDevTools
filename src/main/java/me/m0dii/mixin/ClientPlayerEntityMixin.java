package me.m0dii.mixin;

import com.mojang.authlib.GameProfile;
import me.m0dii.modules.freecam.CameraUtils;
import me.m0dii.modules.freecam.DummyMovementInput;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {

    @Shadow
    public ClientInput input;
    @Unique
    private DummyMovementInput dummyMovementInput;
    @Unique
    private ClientInput realInput;

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Unique
    private boolean isLocalClientPlayer() {
        return (Object) this == Minecraft.getInstance().player;
    }

    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void allowPlayerMovementInFreeCameraMode(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.INSTANCE.isEnabled()
                && this.isLocalClientPlayer()
                && !FreecamModule.INSTANCE.shouldShowPlayerModel()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void disableMovementInputsPre(CallbackInfo ci) {
        if (CameraUtils.shouldPreventPlayerMovement() && this.isLocalClientPlayer()) {
            if (this.dummyMovementInput == null) {
                this.dummyMovementInput = new DummyMovementInput(Minecraft.getInstance().options);
            }
            this.realInput = this.input;
            this.input = this.dummyMovementInput;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void disableMovementInputsPost(CallbackInfo ci) {
        if (this.realInput != null) {
            this.input = this.realInput;
            this.realInput = null;
        }
    }

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void preventWallUnsprint(CallbackInfo ci) {
        if (!TweaksModule.INSTANCE.disableWallUnsprint()) {
            return;
        }

        if (!this.horizontalCollision || this.input == null || this.input.keyPresses == null) {
            return;
        }

        if (!this.input.keyPresses.forward()) {
            return;
        }

        if (this.input.keyPresses.sprint() || TweaksModule.INSTANCE.permanentSprint()) {
            this.setSprinting(true);
        }
    }

}
