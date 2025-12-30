package me.m0dii.nbteditor.mixin;

import me.m0dii.M0DevTools;
import me.m0dii.nbteditor.screens.containers.ClientHandledScreen;
import me.m0dii.nbteditor.server.NBTEditorServer;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "<init>", at = @At("HEAD"))
    private static void init(NetworkSide side, CallbackInfo info) {
        // When on a dedicated server, all threads are already server threads
        if (side == NetworkSide.SERVERBOUND) {
            NBTEditorServer.registerServerThread(Thread.currentThread());
        }
    }

    @Shadow
    public abstract NetworkSide getSide();

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo info) {
        if (getSide() != NetworkSide.CLIENTBOUND) {
            return;
        }

        if (MiscUtil.client.currentScreen instanceof ClientHandledScreen) {
            if (packet instanceof ClickSlotC2SPacket slotPacket) {
                info.cancel();
                M0DevTools.LOGGER.warn("Tried to send a slot click packet while on a ClientHandledScreen: slot=" +
                        slotPacket.getSlot() + ", button=" + slotPacket.getButton() + ", action=" + slotPacket.getActionType());
            }
        }
    }

}