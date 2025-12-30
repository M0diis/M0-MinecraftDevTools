package me.m0dii.nbteditor.multiversion.mixin.toggled;

import me.m0dii.nbteditor.misc.MixinLink;
import me.m0dii.nbteditor.multiversion.MVComponentType;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BookScreen.Contents.class)
public class BookScreenContentsMixin {
    @Inject(method = "create", at = @At("RETURN"))
    private static void create(ItemStack item, CallbackInfoReturnable<BookScreen.Contents> info) {
        if (item.contains(MVComponentType.WRITTEN_BOOK_CONTENT)) {
            MixinLink.WRITTEN_BOOK_CONTENTS.put(info.getReturnValue(), true);
        }
    }
}
