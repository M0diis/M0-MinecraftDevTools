package me.m0dii.modules.itemdata;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.m0dii.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ItemDataClientCommand {
    private ItemDataClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommands.literal("itemdata")
                        .executes(context -> openDefault(context.getSource()))
                        .then(ClientCommands.literal("mainhand")
                                .executes(context -> openSlot(context.getSource(), context.getSource().getClient().player == null
                                        ? -1
                                        : context.getSource().getClient().player.getInventory().getSelectedSlot())))
                        .then(ClientCommands.literal("offhand")
                                .executes(context -> openSlot(context.getSource(), Inventory.SLOT_OFFHAND)))
                        .then(ClientCommands.literal("slot")
                                .then(ClientCommands.argument("slot", IntegerArgumentType.integer(0, Inventory.SLOT_OFFHAND))
                                        .executes(context -> openSlot(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))))));
    }

    public static int openDefault(FabricClientCommandSource source) {
        Minecraft client = source.getClient();
        if (client.player == null || client.level == null) {
            source.sendError(Component.literal("You must be in a world to use this command."));
            return 0;
        }

        int hoveredSlot = findHoveredPlayerInventorySlot(client);
        if (hoveredSlot >= 0) {
            return openSlot(source, hoveredSlot);
        }
        return openSlot(source, client.player.getInventory().getSelectedSlot());
    }

    public static int openSlot(FabricClientCommandSource source, int slotIndex) {
        Minecraft client = source.getClient();
        if (client.player == null || client.level == null) {
            source.sendError(Component.literal("You must be in a world to use this command."));
            return 0;
        }
        if (slotIndex < 0 || slotIndex > Inventory.SLOT_OFFHAND) {
            source.sendError(Component.literal("Invalid inventory slot."));
            return 0;
        }

        ItemStack localStack = client.player.getInventory().getItem(slotIndex);
        if (localStack == null || localStack.isEmpty()) {
            source.sendError(Component.literal("The target inventory slot is empty."));
            return 0;
        }

        ItemDataReference reference = new ItemDataReference(slotIndex, describeSlot(slotIndex, client.player.getInventory().getSelectedSlot()), localStack.getHoverName().getString());
        boolean sent = ItemDataSyncClient.requestItemData(slotIndex, payload -> {
            if (client.player == null) {
                return;
            }
            if (!payload.found() || payload.itemData() == null || payload.itemData().isEmpty()) {
                client.player.sendSystemMessage(Component.literal("[ItemData] The server could not provide data for " + reference.slotLabel() + "."));
                return;
            }
            client.gui.setScreen(new ItemDataScreen(client.gui.screen(), reference, payload.itemData()));
        });

        if (!sent) {
            source.sendError(Component.literal("Unable to request item data from the server."));
            return 0;
        }

        source.sendFeedback(Component.literal("Requesting item data for " + reference.slotLabel() + "..."));
        return 1;
    }

    private static int findHoveredPlayerInventorySlot(Minecraft client) {
        if (client.player == null || !(client.gui.screen() instanceof AbstractContainerScreen<?> handledScreen)) {
            return -1;
        }

        double mouseX = client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight();
        Slot slot = ((HandledScreenAccessor) handledScreen).m0dev$invokeGetSlotAt(mouseX, mouseY);
        if (slot == null || slot.container != client.player.getInventory() || !slot.hasItem()) {
            return -1;
        }
        return slot.getContainerSlot();
    }

    private static String describeSlot(int slotIndex, int selectedSlot) {
        if (slotIndex == Inventory.SLOT_OFFHAND) {
            return "Offhand";
        }
        if (slotIndex == selectedSlot) {
            return "Main Hand";
        }
        if (slotIndex < Inventory.SELECTION_SIZE) {
            return "Hotbar " + (slotIndex + 1);
        }
        if (slotIndex < Inventory.INVENTORY_SIZE) {
            return "Inventory " + (slotIndex - Inventory.SELECTION_SIZE + 1);
        }
        return "Slot " + slotIndex;
    }
}
