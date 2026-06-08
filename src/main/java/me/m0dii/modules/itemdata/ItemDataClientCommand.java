package me.m0dii.modules.itemdata;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.m0dii.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

public final class ItemDataClientCommand {
    private ItemDataClientCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("itemdata")
                        .executes(context -> openDefault(context.getSource()))
                        .then(ClientCommandManager.literal("mainhand")
                                .executes(context -> openSlot(context.getSource(), context.getSource().getClient().player == null
                                        ? -1
                                        : context.getSource().getClient().player.getInventory().getSelectedSlot())))
                        .then(ClientCommandManager.literal("offhand")
                                .executes(context -> openSlot(context.getSource(), PlayerInventory.OFF_HAND_SLOT)))
                        .then(ClientCommandManager.literal("slot")
                                .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(0, PlayerInventory.OFF_HAND_SLOT))
                                        .executes(context -> openSlot(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "slot")
                                        ))))));
    }

    public static int openDefault(FabricClientCommandSource source) {
        MinecraftClient client = source.getClient();
        if (client.player == null || client.world == null) {
            source.sendError(Text.literal("You must be in a world to use this command."));
            return 0;
        }

        int hoveredSlot = findHoveredPlayerInventorySlot(client);
        if (hoveredSlot >= 0) {
            return openSlot(source, hoveredSlot);
        }
        return openSlot(source, client.player.getInventory().getSelectedSlot());
    }

    public static int openSlot(FabricClientCommandSource source, int slotIndex) {
        MinecraftClient client = source.getClient();
        if (client.player == null || client.world == null) {
            source.sendError(Text.literal("You must be in a world to use this command."));
            return 0;
        }
        if (slotIndex < 0 || slotIndex > PlayerInventory.OFF_HAND_SLOT) {
            source.sendError(Text.literal("Invalid inventory slot."));
            return 0;
        }

        ItemStack localStack = client.player.getInventory().getStack(slotIndex);
        if (localStack == null || localStack.isEmpty()) {
            source.sendError(Text.literal("The target inventory slot is empty."));
            return 0;
        }

        ItemDataReference reference = new ItemDataReference(slotIndex, describeSlot(slotIndex, client.player.getInventory().getSelectedSlot()), localStack.getName().getString());
        boolean sent = ItemDataSyncClient.requestItemData(slotIndex, payload -> {
            if (client.player == null) {
                return;
            }
            if (!payload.found() || payload.itemData() == null || payload.itemData().isEmpty()) {
                client.player.sendMessage(Text.literal("[ItemData] The server could not provide data for " + reference.slotLabel() + "."), false);
                return;
            }
            client.setScreen(new ItemDataScreen(client.currentScreen, reference, payload.itemData()));
        });

        if (!sent) {
            source.sendError(Text.literal("Unable to request item data from the server."));
            return 0;
        }

        source.sendFeedback(Text.literal("Requesting item data for " + reference.slotLabel() + "..."));
        return 1;
    }

    private static int findHoveredPlayerInventorySlot(MinecraftClient client) {
        if (client.player == null || !(client.currentScreen instanceof HandledScreen<?> handledScreen)) {
            return -1;
        }

        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
        Slot slot = ((HandledScreenAccessor) handledScreen).m0dev$invokeGetSlotAt(mouseX, mouseY);
        if (slot == null || slot.inventory != client.player.getInventory() || !slot.hasStack()) {
            return -1;
        }
        return slot.getIndex();
    }

    private static String describeSlot(int slotIndex, int selectedSlot) {
        if (slotIndex == PlayerInventory.OFF_HAND_SLOT) {
            return "Offhand";
        }
        if (slotIndex == selectedSlot) {
            return "Main Hand";
        }
        if (slotIndex < PlayerInventory.HOTBAR_SIZE) {
            return "Hotbar " + (slotIndex + 1);
        }
        if (slotIndex < PlayerInventory.MAIN_SIZE) {
            return "Inventory " + (slotIndex - PlayerInventory.HOTBAR_SIZE + 1);
        }
        return "Slot " + slotIndex;
    }
}
