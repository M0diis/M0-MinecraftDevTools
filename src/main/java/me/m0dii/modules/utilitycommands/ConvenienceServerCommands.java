package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public final class ConvenienceServerCommands {
    private ConvenienceServerCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("anvil").executes(context -> openAnvil(context.getSource())));
        dispatcher.register(literal("cartography").executes(context -> openCartography(context.getSource())));
        dispatcher.register(literal("craft").executes(context -> openCrafting(context.getSource())));
        dispatcher.register(literal("ec").executes(context -> openEnderChest(context.getSource())));
        dispatcher.register(literal("grindstone").executes(context -> openGrindstone(context.getSource())));
        dispatcher.register(literal("loom").executes(context -> openLoom(context.getSource())));
        dispatcher.register(literal("smithing").executes(context -> openSmithing(context.getSource())));
        dispatcher.register(literal("stonecutter").executes(context -> openStonecutter(context.getSource())));
        dispatcher.register(literal("trash").executes(context -> openTrash(context.getSource())));
    }

    private static int openAnvil(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Anvil"), AlwaysUsableAnvilScreenHandler::new);
    }

    private static int openCartography(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Cartography Table"), AlwaysUsableCartographyScreenHandler::new);
    }

    private static int openCrafting(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Crafting"), AlwaysUsableCraftingScreenHandler::new);
    }

    private static int openEnderChest(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Ender Chest"),
                (syncId, inventory, player) -> new AlwaysUsableGenericContainerScreenHandler(
                        ScreenHandlerType.GENERIC_9X3,
                        syncId,
                        inventory,
                        player.getEnderChestInventory(),
                        3
                ));
    }

    private static int openGrindstone(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Grindstone"), AlwaysUsableGrindstoneScreenHandler::new);
    }

    private static int openLoom(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Loom"), AlwaysUsableLoomScreenHandler::new);
    }

    private static int openSmithing(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Smithing Table"), AlwaysUsableSmithingScreenHandler::new);
    }

    private static int openStonecutter(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Stonecutter"), AlwaysUsableStonecutterScreenHandler::new);
    }

    private static int openTrash(ServerCommandSource source) throws CommandSyntaxException {
        return openScreen(source, Text.literal("Trash"),
                (syncId, inventory, player) -> new TrashScreenHandler(syncId, inventory, new SimpleInventory(54)));
    }

    private static int openScreen(ServerCommandSource source, Text title, ScreenFactory factory) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> factory.create(syncId, inventory, player),
                title
        ));
        return 1;
    }

    @FunctionalInterface
    private interface ScreenFactory {
        ScreenHandler create(int syncId, PlayerInventory inventory, ServerPlayerEntity player);
    }

    private static final class AlwaysUsableAnvilScreenHandler extends AnvilScreenHandler {
        private AlwaysUsableAnvilScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class AlwaysUsableCartographyScreenHandler extends CartographyTableScreenHandler {
        private AlwaysUsableCartographyScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class AlwaysUsableCraftingScreenHandler extends CraftingScreenHandler {
        private AlwaysUsableCraftingScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static class AlwaysUsableGenericContainerScreenHandler extends GenericContainerScreenHandler {
        private AlwaysUsableGenericContainerScreenHandler(ScreenHandlerType<?> type,
                                                          int syncId,
                                                          PlayerInventory playerInventory,
                                                          Inventory inventory,
                                                          int rows) {
            super(type, syncId, playerInventory, inventory, rows);
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class AlwaysUsableGrindstoneScreenHandler extends GrindstoneScreenHandler {
        private AlwaysUsableGrindstoneScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class AlwaysUsableLoomScreenHandler extends LoomScreenHandler {
        private AlwaysUsableLoomScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class AlwaysUsableSmithingScreenHandler extends SmithingScreenHandler {
        private AlwaysUsableSmithingScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class AlwaysUsableStonecutterScreenHandler extends StonecutterScreenHandler {
        private AlwaysUsableStonecutterScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
            super(syncId, inventory, ScreenHandlerContext.create(player.getEntityWorld(), player.getBlockPos()));
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    private static final class TrashScreenHandler extends AlwaysUsableGenericContainerScreenHandler {
        private final SimpleInventory trashInventory;

        private TrashScreenHandler(int syncId, PlayerInventory playerInventory, SimpleInventory trashInventory) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, trashInventory, 6);
            this.trashInventory = trashInventory;
        }

        @Override
        public void onClosed(PlayerEntity player) {
            this.trashInventory.clear();
            super.onClosed(player);
        }
    }
}
