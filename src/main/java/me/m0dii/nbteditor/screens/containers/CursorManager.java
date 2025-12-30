package me.m0dii.nbteditor.screens.containers;

import lombok.Getter;
import me.m0dii.nbteditor.multiversion.networking.ClientNetworking;
import me.m0dii.nbteditor.packets.SetCursorC2SPacket;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class CursorManager {

    @Getter
    private HandledScreen<?> currentRoot;
    private boolean currentRootIsInventory;
    private boolean currentRootHasServerCursor;
    @Getter
    private boolean currentRootClosed;
    @Getter
    private HandledScreen<?> currentBranch;

    public CursorManager() {
    }

    public boolean isBranched() {
        return currentRoot != null && currentRoot != currentBranch;
    }

    public void onNoScreenSet() {
        currentRoot = null;
        currentRootClosed = false;
        currentBranch = null;
    }

    public void onHandledScreenSet(HandledScreen<?> screen) {
        if (screen == currentBranch) {
            return;
        }

        currentRoot = screen;
        currentRootIsInventory = (currentRoot.getScreenHandler() == MiscUtil.client.player.playerScreenHandler ||
                currentRoot instanceof CreativeInventoryScreen);
        currentRootHasServerCursor = !(screen instanceof CreativeInventoryScreen);
        currentRootClosed = false;
        currentBranch = screen;
    }

    public void onCloseScreenPacket() {
        if (currentRoot == null || currentRootIsInventory) {
            return;
        }

        currentRootClosed = true;
    }

    private void transferCursorTo(HandledScreen<?> branch) {
        if (currentBranch == branch) {
            return;
        }

        ScreenHandler handler = branch.getScreenHandler();
        ScreenHandler currentHandler = currentBranch.getScreenHandler();

        handler.setCursorStack(currentHandler.getCursorStack());
        handler.setPreviousCursorStack(handler.getCursorStack());

        currentHandler.setCursorStack(ItemStack.EMPTY);
        currentHandler.setPreviousCursorStack(ItemStack.EMPTY);

        if (currentRootHasServerCursor) {
            if (branch == currentRoot) {
                ClientNetworking.send(new SetCursorC2SPacket(handler.getCursorStack()));
            } else if (currentBranch == currentRoot) {
                ClientNetworking.send(new SetCursorC2SPacket(ItemStack.EMPTY));
            }
        }
    }

    public void showBranch(HandledScreen<?> branch) {
        if (currentRoot == null) {
            if (MiscUtil.client.interactionManager.hasCreativeInventory()) {
                currentRoot = new CreativeInventoryScreen(
                        MiscUtil.client.player,
                        MiscUtil.client.player.networkHandler.getEnabledFeatures(),
                        MiscUtil.client.options.getOperatorItemsTab().getValue()
                );

                currentRootHasServerCursor = false;
            } else {
                currentRoot = new InventoryScreen(MiscUtil.client.player);
                currentRootHasServerCursor = true;
            }
            currentRootIsInventory = true;
            currentRootClosed = false;
            currentBranch = currentRoot;
        }
        if (branch == null) {
            branch = currentRoot;
        }

        if (currentRootClosed && branch == currentRoot) {
            closeRoot();
            return;
        }

        transferCursorTo(branch);
        currentBranch = branch;
        MiscUtil.client.player.currentScreenHandler = branch.getScreenHandler();
        branch.cancelNextRelease = true;
        MiscUtil.client.setScreen(branch);
    }

    public void showRoot() {
        showBranch(currentRoot);
    }

    public void closeRoot() {
        if (currentRoot == null) {
            MiscUtil.client.setScreen(null);
            return;
        }

        if (currentRootClosed) {
            if (currentBranch != currentRoot) {
                ItemStack cursor = currentBranch.getScreenHandler().getCursorStack();
                if (currentRootHasServerCursor) {
                    MiscUtil.get(cursor, true);
                    cursor = ItemStack.EMPTY;
                }
                currentRoot.getScreenHandler().setCursorStack(cursor);
                currentRoot.getScreenHandler().setPreviousCursorStack(cursor);
            }
            MiscUtil.client.player.closeScreen(); // will trigger #onNoScreenSet()
            return;
        }

        transferCursorTo(currentRoot);
        MiscUtil.client.player.closeHandledScreen(); // will trigger #onNoScreenSet()
    }

    public void setCursor(ItemStack item) {
        if (currentRoot == null) {
            throw new IllegalStateException("There is no root to set the cursor of");
        }

        currentBranch.getScreenHandler().setCursorStack(item);
        currentBranch.getScreenHandler().setPreviousCursorStack(item);

        if (currentRootHasServerCursor && currentBranch == currentRoot) {
            ClientNetworking.send(new SetCursorC2SPacket(item));
        }
    }

}
