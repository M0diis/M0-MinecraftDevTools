package bettercommandblockui.mixin;

import bettercommandblockui.main.ui.screen.BetterMinecartCommandBlockScreen;
import me.m0dii.modules.commandblockui.BetterCommandBlockUiModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecartCommandBlock.class)
public class CommandBlockMinecartMixin {
    @Redirect(method = "interact",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMinecartCommandBlock(Lnet/minecraft/world/entity/vehicle/minecart/MinecartCommandBlock;)V"))
    public void openBetterMinecartCommandBlockScreen(Player instance, MinecartCommandBlock minecart) {
        if (!BetterCommandBlockUiModule.INSTANCE.isEnabled()) {
            instance.openMinecartCommandBlock(minecart);
            return;
        }
        if (instance instanceof LocalPlayer) {
            Minecraft client = ((ClientPlayerEntityAccessor) instance).getMinecraft();
            client.gui.setScreen(new BetterMinecartCommandBlockScreen(minecart));
        }
    }
}
