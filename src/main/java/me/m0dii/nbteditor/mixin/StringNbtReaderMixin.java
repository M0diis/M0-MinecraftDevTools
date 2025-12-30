package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.util.NbtFormatter;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.StringNbtReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringNbtReader.class)
public class StringNbtReaderMixin {
    @Inject(method = "parsePrimitive", at = @At("HEAD"), cancellable = true)
    private void parsePrimitive(String input, CallbackInfoReturnable<NbtElement> info) {
        if (ConfigScreen.isSpecialNumbers() && MixinLink.specialNumbers.contains(Thread.currentThread())) {
            Number specialNum = NbtFormatter.SPECIAL_NUMS.get(input);
            if (specialNum != null) {
                switch (specialNum) {
                    case Double d -> info.setReturnValue(NbtDouble.of(d));
                    case Float f -> info.setReturnValue(NbtFloat.of(f));
                    default ->
                            throw new IllegalStateException("Number of invalid type: " + specialNum.getClass().getName());
                }
            }
        }
    }
}
