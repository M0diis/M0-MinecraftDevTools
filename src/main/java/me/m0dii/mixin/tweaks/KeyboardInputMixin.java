package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void applyPermanentSneakAndSprint(CallbackInfo ci) {
        if (this.keyPresses == null) {
            return;
        }

        boolean sneak = this.keyPresses.shift() || TweaksModule.INSTANCE.permanentSneak();
        boolean sprint = this.keyPresses.sprint() || TweaksModule.INSTANCE.permanentSprint();

        if (sneak == this.keyPresses.shift() && sprint == this.keyPresses.sprint()) {
            return;
        }

        this.keyPresses = new Input(
                this.keyPresses.forward(),
                this.keyPresses.backward(),
                this.keyPresses.left(),
                this.keyPresses.right(),
                this.keyPresses.jump(),
                sneak,
                sprint
        );
    }
}
