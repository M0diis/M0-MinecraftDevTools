package me.m0dii.modules.utilitycommands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;

import static net.minecraft.commands.Commands.literal;

public final class ConvenienceServerCommands {
    private ConvenienceServerCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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

    private static int openAnvil(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Anvil"), AlwaysUsableAnvilScreenHandler::new);
    }

    private static int openCartography(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Cartography Table"), AlwaysUsableCartographyScreenHandler::new);
    }

    private static int openCrafting(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Crafting"), AlwaysUsableCraftingScreenHandler::new);
    }

    private static int openEnderChest(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Ender Chest"),
                (syncId, inventory, player) -> new AlwaysUsableGenericContainerScreenHandler(
                        MenuType.GENERIC_9x3,
                        syncId,
                        inventory,
                        player.getEnderChestInventory(),
                        3
                ));
    }

    private static int openGrindstone(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Grindstone"), AlwaysUsableGrindstoneScreenHandler::new);
    }

    private static int openLoom(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Loom"), AlwaysUsableLoomScreenHandler::new);
    }

    private static int openSmithing(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Smithing Table"), AlwaysUsableSmithingScreenHandler::new);
    }

    private static int openStonecutter(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Stonecutter"), AlwaysUsableStonecutterScreenHandler::new);
    }

    private static int openTrash(CommandSourceStack source) throws CommandSyntaxException {
        return openScreen(source, Component.literal("Trash"),
                (syncId, inventory, player) -> new TrashScreenHandler(syncId, inventory, new SimpleContainer(54)));
    }

    private static int openScreen(CommandSourceStack source, Component title, ScreenFactory factory) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, ignored) -> factory.create(syncId, inventory, player),
                title
        ));
        return 1;
    }

    @FunctionalInterface
    private interface ScreenFactory {
        AbstractContainerMenu create(int syncId, Inventory inventory, ServerPlayer player);
    }

    private static final class AlwaysUsableAnvilScreenHandler extends AnvilMenu {
        private AlwaysUsableAnvilScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class AlwaysUsableCartographyScreenHandler extends CartographyTableMenu {
        private AlwaysUsableCartographyScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class AlwaysUsableCraftingScreenHandler extends CraftingMenu {
        private AlwaysUsableCraftingScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static class AlwaysUsableGenericContainerScreenHandler extends ChestMenu {
        private AlwaysUsableGenericContainerScreenHandler(MenuType<?> type,
                                                          int syncId,
                                                          Inventory playerInventory,
                                                          Container inventory,
                                                          int rows) {
            super(type, syncId, playerInventory, inventory, rows);
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class AlwaysUsableGrindstoneScreenHandler extends GrindstoneMenu {
        private AlwaysUsableGrindstoneScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class AlwaysUsableLoomScreenHandler extends LoomMenu {
        private AlwaysUsableLoomScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class AlwaysUsableSmithingScreenHandler extends SmithingMenu {
        private AlwaysUsableSmithingScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class AlwaysUsableStonecutterScreenHandler extends StonecutterMenu {
        private AlwaysUsableStonecutterScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
            super(syncId, inventory, ContainerLevelAccess.create(player.level(), player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    private static final class TrashScreenHandler extends AlwaysUsableGenericContainerScreenHandler {
        private final SimpleContainer trashInventory;

        private TrashScreenHandler(int syncId, Inventory playerInventory, SimpleContainer trashInventory) {
            super(MenuType.GENERIC_9x6, syncId, playerInventory, trashInventory, 6);
            this.trashInventory = trashInventory;
        }

        @Override
        public void removed(Player player) {
            this.trashInventory.clearContent();
            super.removed(player);
        }
    }
}
