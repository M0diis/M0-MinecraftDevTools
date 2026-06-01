package me.m0dii.mixin.tweaks;

import me.m0dii.modules.tweaks.TweaksModule;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.TickableSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At("HEAD"),
            cancellable = true)
    private void disableSounds(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (TweaksModule.INSTANCE.disableSounds()) {
            cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At("HEAD"), cancellable = true)
    private void disableDelayedSounds(SoundInstance sound, int delay, CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableSounds()) {
            ci.cancel();
        }
    }

    @Inject(method = "playNextTick", at = @At("HEAD"), cancellable = true)
    private void disableNextTickSounds(TickableSoundInstance sound, CallbackInfo ci) {
        if (TweaksModule.INSTANCE.disableSounds()) {
            ci.cancel();
        }
    }
}
