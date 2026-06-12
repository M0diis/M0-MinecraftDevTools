package me.m0dii.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("header")
    Text m0dev$getHeader();

    @Accessor("footer")
    Text m0dev$getFooter();
}
