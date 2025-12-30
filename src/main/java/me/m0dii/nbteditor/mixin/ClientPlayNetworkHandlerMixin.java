package me.m0dii.nbteditor.mixin;

import me.m0dii.M0DevTools;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.multiversion.IgnoreCloseScreenPacket;
import me.m0dii.nbteditor.screens.containers.ClientHandledScreen;
import me.m0dii.nbteditor.screens.containers.ClientScreenHandler;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    private static boolean updatingClientInventory;

    @Inject(method = "onInventory", at = @At("HEAD"), cancellable = true)
    private void onInventory(InventoryS2CPacket packet, CallbackInfo info) {
        if (!MiscUtil.client.isOnThread() || updatingClientInventory) {
            return;
        }

        if (packet.getSyncId() == ClientScreenHandler.SYNC_ID) {
            M0DevTools.LOGGER.warn("Ignoring an inventory packet with a ClientHandledScreen sync id!");
            info.cancel();
            return;
        }

        if (M0DevToolsClient.CURSOR_MANAGER.isBranched()) {
            info.cancel();

            try {
                updatingClientInventory = true;
                MiscUtil.client.player.currentScreenHandler = M0DevToolsClient.CURSOR_MANAGER.getCurrentRoot().getScreenHandler();
                ((ClientPlayNetworkHandler) (Object) this).onInventory(packet);
            } finally {
                updatingClientInventory = false;
                MiscUtil.client.player.currentScreenHandler = M0DevToolsClient.CURSOR_MANAGER.getCurrentBranch().getScreenHandler();
            }
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("HEAD"), cancellable = true)
    private void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        if (!MiscUtil.client.isOnThread() || updatingClientInventory) {
            return;
        }

        if (packet.getSyncId() == ClientScreenHandler.SYNC_ID) {
            M0DevTools.LOGGER.warn("Ignoring a slot update packet with a ClientHandledScreen sync id!");
            info.cancel();
            return;
        }

        if (M0DevToolsClient.CURSOR_MANAGER.isBranched()) {
            info.cancel();

            if (packet.getSyncId() == -1) {
                if (!(M0DevToolsClient.CURSOR_MANAGER.getCurrentRoot() instanceof CreativeInventoryScreen)) {
                    MiscUtil.client.player.currentScreenHandler.setCursorStack(packet.getStack());
                }
                return;
            }

            try {
                updatingClientInventory = true;
                MiscUtil.client.player.currentScreenHandler = M0DevToolsClient.CURSOR_MANAGER.getCurrentRoot().getScreenHandler();
                ((ClientPlayNetworkHandler) (Object) this).onScreenHandlerSlotUpdate(packet);
            } finally {
                updatingClientInventory = false;
                MiscUtil.client.player.currentScreenHandler = M0DevToolsClient.CURSOR_MANAGER.getCurrentBranch().getScreenHandler();
            }
        }
    }

    @Inject(method = "onInventory", at = @At("RETURN"), cancellable = true)
    private void onInventory_return(InventoryS2CPacket packet, CallbackInfo info) {
        if (MiscUtil.client.currentScreen instanceof ClientHandledScreen clientHandledScreen) {
            clientHandledScreen.getServerInventoryManager().onInventoryPacket(packet);
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"), cancellable = true)
    private void onScreenHandlerSlotUpdate_return(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo info) {
        if (MiscUtil.client.currentScreen instanceof ClientHandledScreen clientHandledScreen) {
            clientHandledScreen.getServerInventoryManager().onScreenHandlerSlotUpdatePacket(packet);
        }
    }

    @Inject(method = "onCloseScreen", at = @At("HEAD"), cancellable = true)
    private void onCloseScreen(CloseScreenS2CPacket packet, CallbackInfo info) {
        if (!MiscUtil.client.isOnThread()) {
            return;
        }

        if (packet.getSyncId() == ClientScreenHandler.SYNC_ID) {
            M0DevTools.LOGGER.warn("Ignoring a close screen packet with a ClientHandledScreen sync id!");
            info.cancel();
            return;
        }

        M0DevToolsClient.CURSOR_MANAGER.onCloseScreenPacket();

        if (MiscUtil.client.currentScreen instanceof IgnoreCloseScreenPacket) {
            info.cancel();
        }
    }

}
