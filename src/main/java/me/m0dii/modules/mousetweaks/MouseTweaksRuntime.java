package me.m0dii.modules.mousetweaks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class MouseTweaksRuntime {
    private static HandledScreen<?> openScreen;
    private static MouseTweaksScreenHandler handler;
    private static boolean disableWheelForThisContainer;
    private static Slot oldSelectedSlot;
    private static double accumulatedScrollDelta;
    private static boolean canDoLmbDrag;
    private static boolean canDoRmbDrag;
    private static boolean rmbTweakLeftOriginalSlot;

    private MouseTweaksRuntime() {
    }

    public static void reset() {
        openScreen = null;
        handler = null;
        disableWheelForThisContainer = false;
        oldSelectedSlot = null;
        accumulatedScrollDelta = 0;
        canDoLmbDrag = false;
        canDoRmbDrag = false;
        rmbTweakLeftOriginalSlot = false;
    }

    public static boolean onMouseClicked(HandledScreen<?> screen, Click click) {
        MouseTweaksButton button = MouseTweaksButton.fromButton(click.button());
        if (button == null) {
            return false;
        }

        updateScreen(screen);
        if (handler == null || !MouseTweaksModule.INSTANCE.isEnabled()) {
            return false;
        }

        oldSelectedSlot = handler.getSlotUnderMouse(click.x(), click.y());
        ItemStack stackOnMouse = cursorStack();

        if (button == MouseTweaksButton.LEFT) {
            if (stackOnMouse.isEmpty()) {
                canDoLmbDrag = true;
            }
        } else if (button == MouseTweaksButton.RIGHT) {
            if (stackOnMouse.isEmpty() || !MouseTweaksModule.INSTANCE.rmbTweak()) {
                return false;
            }
            canDoRmbDrag = true;
            rmbTweakLeftOriginalSlot = false;
        }

        return false;
    }

    public static boolean onMouseReleased(HandledScreen<?> screen, Click click) {
        MouseTweaksButton button = MouseTweaksButton.fromButton(click.button());
        if (button == null) {
            return false;
        }

        updateScreen(screen);
        if (handler == null || !MouseTweaksModule.INSTANCE.isEnabled()) {
            return false;
        }

        if (button == MouseTweaksButton.LEFT) {
            canDoLmbDrag = false;
        } else if (button == MouseTweaksButton.RIGHT) {
            canDoRmbDrag = false;
        }

        return false;
    }

    public static boolean onMouseDragged(HandledScreen<?> screen, Click click) {
        MouseTweaksButton button = MouseTweaksButton.fromButton(click.button());
        if (button == null) {
            return false;
        }

        updateScreen(screen);
        if (handler == null || !MouseTweaksModule.INSTANCE.isEnabled()) {
            return false;
        }

        Slot selectedSlot = handler.getSlotUnderMouse(click.x(), click.y());
        if (selectedSlot == oldSelectedSlot) {
            return false;
        }

        ItemStack stackOnMouse = cursorStack();
        if (canDoRmbDrag && button == MouseTweaksButton.RIGHT && !rmbTweakLeftOriginalSlot) {
            rmbTweakLeftOriginalSlot = true;
            handler.disableRmbDraggingFunctionality();
            rmbTweakMaybeClickSlot(oldSelectedSlot, stackOnMouse);
        }

        oldSelectedSlot = selectedSlot;
        if (selectedSlot == null || handler.isIgnored(selectedSlot)) {
            return false;
        }

        if (button == MouseTweaksButton.LEFT) {
            if (!canDoLmbDrag) {
                return false;
            }

            ItemStack selectedSlotStack = selectedSlot.getStack();
            if (selectedSlotStack.isEmpty()) {
                return false;
            }

            boolean shiftIsDown = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (stackOnMouse.isEmpty()) {
                if (!MouseTweaksModule.INSTANCE.lmbTweakWithoutItem() || !shiftIsDown) {
                    return false;
                }
                handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, true);
            } else {
                if (!MouseTweaksModule.INSTANCE.lmbTweakWithItem()) {
                    return false;
                }
                if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) {
                    return false;
                }
                if (shiftIsDown) {
                    handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, true);
                } else {
                    if (stackOnMouse.getCount() + selectedSlotStack.getCount() > stackOnMouse.getMaxCount()) {
                        return false;
                    }
                    handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
                    if (!handler.isCraftingOutput(selectedSlot)) {
                        handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
                    }
                }
            }
        } else if (button == MouseTweaksButton.RIGHT) {
            if (!canDoRmbDrag) {
                return false;
            }
            rmbTweakMaybeClickSlot(selectedSlot, stackOnMouse);
        }

        return false;
    }

    public static boolean onMouseScrolled(HandledScreen<?> screen, double mouseX, double mouseY, double verticalAmount) {
        updateScreen(screen);
        if (handler == null || !MouseTweaksModule.INSTANCE.isEnabled() || disableWheelForThisContainer || !MouseTweaksModule.INSTANCE.wheelTweak()) {
            return false;
        }

        Slot selectedSlot = handler.getSlotUnderMouse(mouseX, mouseY);
        if (selectedSlot == null || handler.isIgnored(selectedSlot)) {
            return false;
        }

        ItemStack selectedSlotStack = selectedSlot.getStack();
        if (selectedSlotStack.getItem() instanceof BundleItem) {
            return false;
        }

        double scaledDelta = MouseTweaksModule.INSTANCE.scrollItemScaling().scale(verticalAmount);
        if (accumulatedScrollDelta != 0 && Math.signum(scaledDelta) != Math.signum(accumulatedScrollDelta)) {
            accumulatedScrollDelta = 0;
        }

        accumulatedScrollDelta += scaledDelta;
        int delta = (int) accumulatedScrollDelta;
        accumulatedScrollDelta -= delta;
        if (delta == 0) {
            return true;
        }

        List<Slot> slots = handler.getSlots();
        int numItemsToMove = Math.abs(delta);
        boolean pushItems = delta < 0;
        if (MouseTweaksModule.INSTANCE.wheelScrollDirection().isPositionAware() && otherInventoryIsAbove(selectedSlot, slots)) {
            pushItems = !pushItems;
        }
        if (MouseTweaksModule.INSTANCE.wheelScrollDirection().isInverted()) {
            pushItems = !pushItems;
        }

        if (selectedSlotStack.isEmpty()) {
            return true;
        }

        ItemStack stackOnMouse = cursorStack();
        if (handler.isCraftingOutput(selectedSlot)) {
            if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) {
                return true;
            }

            if (stackOnMouse.isEmpty()) {
                if (!pushItems) {
                    return true;
                }

                while (numItemsToMove-- > 0) {
                    List<Slot> targetSlots = findPushSlots(slots, selectedSlot, selectedSlotStack.getCount(), true);
                    if (targetSlots == null) {
                        break;
                    }

                    handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
                    for (int i = 0; i < targetSlots.size(); i++) {
                        Slot slot = targetSlots.get(i);
                        if (i == targetSlots.size() - 1) {
                            handler.clickSlot(slot, MouseTweaksButton.LEFT, false);
                        } else {
                            int clickTimes = slot.getMaxItemCount(slot.getStack()) - slot.getStack().getCount();
                            while (clickTimes-- > 0) {
                                handler.clickSlot(slot, MouseTweaksButton.RIGHT, false);
                            }
                        }
                    }
                }
            } else {
                while (numItemsToMove-- > 0) {
                    handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
                }
            }
            return true;
        }

        if (!stackOnMouse.isEmpty() && areStacksCompatible(selectedSlotStack, stackOnMouse)) {
            return true;
        }

        if (pushItems) {
            if (!stackOnMouse.isEmpty() && !selectedSlot.canInsert(stackOnMouse)) {
                return true;
            }

            numItemsToMove = Math.min(numItemsToMove, selectedSlotStack.getCount());
            List<Slot> targetSlots = findPushSlots(slots, selectedSlot, numItemsToMove, false);
            if (targetSlots == null || targetSlots.isEmpty()) {
                return true;
            }

            handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
            for (Slot slot : targetSlots) {
                int clickTimes = slot.getMaxItemCount(slot.getStack()) - slot.getStack().getCount();
                clickTimes = Math.min(clickTimes, numItemsToMove);
                numItemsToMove -= clickTimes;
                while (clickTimes-- > 0) {
                    handler.clickSlot(slot, MouseTweaksButton.RIGHT, false);
                }
            }
            handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
            return true;
        }

        int maxItemsToMove = selectedSlot.getMaxItemCount(selectedSlotStack) - selectedSlotStack.getCount();
        numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
            Slot targetSlot = findPullSlot(slots, selectedSlot);
            if (targetSlot == null) {
                break;
            }

            int numItemsInTargetSlot = targetSlot.getStack().getCount();
            if (handler.isCraftingOutput(targetSlot)) {
                if (maxItemsToMove < numItemsInTargetSlot) {
                    break;
                }

                maxItemsToMove -= numItemsInTargetSlot;
                numItemsToMove = Math.min(numItemsToMove - 1, maxItemsToMove);
                if (!stackOnMouse.isEmpty() && !selectedSlot.canInsert(stackOnMouse)) {
                    break;
                }

                handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
                handler.clickSlot(targetSlot, MouseTweaksButton.LEFT, false);
                handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
                continue;
            }

            int numItemsToMoveFromTargetSlot = Math.min(numItemsToMove, numItemsInTargetSlot);
            maxItemsToMove -= numItemsToMoveFromTargetSlot;
            numItemsToMove -= numItemsToMoveFromTargetSlot;

            if (!stackOnMouse.isEmpty() && !targetSlot.canInsert(stackOnMouse)) {
                break;
            }

            handler.clickSlot(targetSlot, MouseTweaksButton.LEFT, false);
            if (numItemsToMoveFromTargetSlot == numItemsInTargetSlot) {
                handler.clickSlot(selectedSlot, MouseTweaksButton.LEFT, false);
            } else {
                for (int i = 0; i < numItemsToMoveFromTargetSlot; i++) {
                    handler.clickSlot(selectedSlot, MouseTweaksButton.RIGHT, false);
                }
            }
            handler.clickSlot(targetSlot, MouseTweaksButton.LEFT, false);
        }

        return true;
    }

    private static void updateScreen(HandledScreen<?> newScreen) {
        if (newScreen == openScreen) {
            return;
        }

        reset();
        openScreen = newScreen;
        if (openScreen == null) {
            return;
        }

        handler = findHandler(openScreen);
        if (handler == null) {
            return;
        }

        disableWheelForThisContainer = handler.isWheelTweakDisabled();
        if (handler.isMouseTweaksDisabled()) {
            handler = null;
        }
    }

    private static MouseTweaksScreenHandler findHandler(HandledScreen<?> screen) {
        if (screen instanceof CreativeInventoryScreen creativeInventoryScreen) {
            return new CreativeInventoryMouseTweaksHandler(creativeInventoryScreen);
        }
        return new HandledScreenMouseTweaksHandler(screen);
    }

    private static ItemStack cursorStack() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null ? ItemStack.EMPTY : client.player.currentScreenHandler.getCursorStack();
    }

    private static void rmbTweakMaybeClickSlot(Slot slot, ItemStack stackOnMouse) {
        if (slot == null || stackOnMouse.isEmpty() || handler.isIgnored(slot) || handler.isCraftingOutput(slot)) {
            return;
        }

        if (!(stackOnMouse.getItem() instanceof BundleItem)) {
            ItemStack selectedSlotStack = slot.getStack();
            if (!areStacksCompatible(selectedSlotStack, stackOnMouse)) {
                return;
            }
            if (selectedSlotStack.getCount() == slot.getMaxItemCount(selectedSlotStack)) {
                return;
            }
        }

        handler.clickSlot(slot, MouseTweaksButton.RIGHT, false);
    }

    private static boolean otherInventoryIsAbove(Slot selectedSlot, List<Slot> slots) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        boolean selectedIsInPlayerInventory = selectedSlot.inventory == client.player.getInventory();
        int otherInventorySlotsBelow = 0;
        int otherInventorySlotsAbove = 0;
        for (Slot slot : slots) {
            if ((slot.inventory == client.player.getInventory()) != selectedIsInPlayerInventory) {
                if (slot.y < selectedSlot.y) {
                    otherInventorySlotsAbove++;
                } else {
                    otherInventorySlotsBelow++;
                }
            }
        }
        return otherInventorySlotsAbove > otherInventorySlotsBelow;
    }

    private static boolean areStacksCompatible(ItemStack a, ItemStack b) {
        return a.isEmpty() || b.isEmpty() || ItemStack.areItemsAndComponentsEqual(a, b);
    }

    private static Slot findPullSlot(List<Slot> slots, Slot selectedSlot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return null;
        }

        int startIndex;
        int endIndex;
        int direction;
        if (MouseTweaksModule.INSTANCE.wheelSearchOrder() == MouseTweaksWheelSearchOrder.FIRST_TO_LAST) {
            startIndex = 0;
            endIndex = slots.size();
            direction = 1;
        } else {
            startIndex = slots.size() - 1;
            endIndex = -1;
            direction = -1;
        }

        ItemStack selectedSlotStack = selectedSlot.getStack();
        boolean findInPlayerInventory = selectedSlot.inventory != client.player.getInventory();
        for (int i = startIndex; i != endIndex; i += direction) {
            Slot slot = slots.get(i);
            if (handler.isIgnored(slot)) {
                continue;
            }
            boolean slotInPlayerInventory = slot.inventory == client.player.getInventory();
            if (findInPlayerInventory != slotInPlayerInventory) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || !areStacksCompatible(selectedSlotStack, stack)) {
                continue;
            }
            return slot;
        }
        return null;
    }

    private static List<Slot> findPushSlots(List<Slot> slots, Slot selectedSlot, int itemCount, boolean mustDistributeAll) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return mustDistributeAll ? null : List.of();
        }

        ItemStack selectedSlotStack = selectedSlot.getStack();
        boolean findInPlayerInventory = selectedSlot.inventory != client.player.getInventory();

        List<Slot> targets = new ArrayList<>();
        List<Slot> emptyTargets = new ArrayList<>();
        for (int i = 0; i < slots.size() && itemCount > 0; i++) {
            Slot slot = slots.get(i);
            if (handler.isIgnored(slot)) {
                continue;
            }
            boolean slotInPlayerInventory = slot.inventory == client.player.getInventory();
            if (findInPlayerInventory != slotInPlayerInventory || handler.isCraftingOutput(slot)) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                if (slot.canInsert(selectedSlotStack)) {
                    emptyTargets.add(slot);
                }
                continue;
            }

            if (areStacksCompatible(selectedSlotStack, stack) && stack.getCount() < slot.getMaxItemCount(stack)) {
                targets.add(slot);
                itemCount -= Math.min(itemCount, slot.getMaxItemCount(stack) - stack.getCount());
            }
        }

        for (int i = 0; i < emptyTargets.size() && itemCount > 0; i++) {
            Slot slot = emptyTargets.get(i);
            targets.add(slot);
            itemCount -= Math.min(itemCount, slot.getMaxItemCount());
        }

        if (mustDistributeAll && itemCount > 0) {
            return null;
        }
        return targets;
    }
}
