package me.m0dii.nbteditor.mixin;

import net.minecraft.nbt.visitor.StringNbtWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StringNbtWriter.class)
public interface StringNbtWriterAccessor {
    @Accessor
    StringBuilder getResult();
}
