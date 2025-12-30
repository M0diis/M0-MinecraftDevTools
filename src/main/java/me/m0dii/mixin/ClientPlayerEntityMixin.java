package me.m0dii.mixin;

import com.mojang.authlib.GameProfile;
import me.m0dii.modules.freecam.CameraUtils;
import me.m0dii.modules.freecam.DummyMovementInput;
import me.m0dii.modules.freecam.FreecamModule;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    @Shadow
    public Input input;
    @Unique
    private final DummyMovementInput dummyMovementInput = new DummyMovementInput(null);
    @Unique
    private Input realInput;

    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "isCamera", at = @At("HEAD"), cancellable = true)
    private void allowPlayerMovementInFreeCameraMode(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.INSTANCE.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void disableMovementInputsPre(CallbackInfo ci) {
        if (CameraUtils.shouldPreventPlayerMovement()) {
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

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void preventHandSwing(Hand hand, CallbackInfo ci) {
        if (CameraUtils.shouldPreventPlayerInputs()) {
            ci.cancel();
        }
    }

}
