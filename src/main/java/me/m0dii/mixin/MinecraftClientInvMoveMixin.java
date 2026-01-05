package me.m0dii.mixin;

import me.m0dii.modules.inventorymove.InventoryMoveModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.util.InputUtil.*;

@Mixin(MinecraftClient.class)
public class MinecraftClientInvMoveMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Screen screen = client.currentScreen;

        if (screen instanceof HandledScreen && InventoryMoveModule.INSTANCE.isEnabled()) {
            client.options.forwardKey.setPressed(isKeyPressed(client.getWindow().getHandle(), GLFW_KEY_W));
            client.options.leftKey.setPressed(isKeyPressed(client.getWindow().getHandle(), GLFW_KEY_A));
            client.options.backKey.setPressed(isKeyPressed(client.getWindow().getHandle(), GLFW_KEY_S));
            client.options.rightKey.setPressed(isKeyPressed(client.getWindow().getHandle(), GLFW_KEY_D));
            client.options.jumpKey.setPressed(isKeyPressed(client.getWindow().getHandle(), GLFW_KEY_SPACE));
            client.options.sprintKey.setPressed(isKeyPressed(client.getWindow().getHandle(), GLFW_KEY_LEFT_CONTROL));
        }
    }

}

