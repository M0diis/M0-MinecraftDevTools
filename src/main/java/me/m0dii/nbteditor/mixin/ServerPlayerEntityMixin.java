package me.m0dii.nbteditor.mixin;

import me.m0dii.nbteditor.multiversion.networking.MVServerNetworking;
import me.m0dii.nbteditor.packets.ContainerScreenS2CPacket;
import me.m0dii.nbteditor.server.ServerMainUtil;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "openHandledScreen", at = @At("HEAD"))
    private void openHandledScreen(NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> info) {
        if (factory instanceof LockableContainerBlockEntity ||
                ServerMainUtil.getRootEnclosingClass(factory.getClass()) == ChestBlock.class || // Double chests
                factory instanceof VehicleInventory) {
            MVServerNetworking.send((ServerPlayerEntity) (Object) this, new ContainerScreenS2CPacket());
        }
    }

    @ModifyVariable(method = "openHandledScreen", at = @At("STORE"), ordinal = 0)
    private ScreenHandler openHandledScreen_screenHandler(ScreenHandler screenHandler) {
        ServerPlayerEntity source = (ServerPlayerEntity) (Object) this;
        if (screenHandler instanceof GenericContainerScreenHandler generic && generic.getInventory() == source.getEnderChestInventory()) {
            MVServerNetworking.send(source, new ContainerScreenS2CPacket());
        }
        return screenHandler;
    }

    @Inject(method = "openHorseInventory", at = @At("HEAD"))
    private void openHorseInventory(AbstractHorseEntity horse, Inventory inventory, CallbackInfo info) {
        MVServerNetworking.send((ServerPlayerEntity) (Object) this, new ContainerScreenS2CPacket());
    }
}
