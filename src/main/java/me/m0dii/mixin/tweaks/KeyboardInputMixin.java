package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void applyPermanentSneakAndSprint(CallbackInfo ci) {
        if (this.playerInput == null) {
            return;
        }

        boolean sneak = this.playerInput.sneak() || TweaksModule.INSTANCE.permanentSneak();
        boolean sprint = this.playerInput.sprint() || TweaksModule.INSTANCE.permanentSprint();

        if (sneak == this.playerInput.sneak() && sprint == this.playerInput.sprint()) {
            return;
        }

        this.playerInput = new PlayerInput(
                this.playerInput.forward(),
                this.playerInput.backward(),
                this.playerInput.left(),
                this.playerInput.right(),
                this.playerInput.jump(),
                sneak,
                sprint
        );
    }
}
