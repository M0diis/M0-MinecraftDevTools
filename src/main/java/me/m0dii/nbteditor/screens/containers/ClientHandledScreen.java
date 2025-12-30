package me.m0dii.nbteditor.screens.containers;

import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.containers.ContainerIO;
import me.m0dii.nbteditor.multiversion.*;
import me.m0dii.nbteditor.nbtreferences.itemreferences.InventoryItemReference;
import me.m0dii.nbteditor.nbtreferences.itemreferences.ItemReference;
import me.m0dii.nbteditor.screens.ConfigScreen;
import me.m0dii.nbteditor.screens.NBTEditorScreen;
import me.m0dii.nbteditor.screens.factories.LocalFactoryScreen;
import me.m0dii.nbteditor.tagreferences.ItemTagReferences;
import me.m0dii.nbteditor.tagreferences.specific.data.Enchants;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

public class ClientHandledScreen extends GenericContainerScreen implements OldEventBehavior, IgnoreCloseScreenPacket {

    private static final Identifier TEXTURE = IdentifierInst.of("textures/gui/container/generic_54.png");
    private ServerInventoryManager serverInv;

    protected ClientHandledScreen(GenericContainerScreenHandler handler, Text title) {
        super(handler, MiscUtil.client.player.getInventory(), title);
        handler.disableSyncing();
    }

    public static boolean handleKeybind(int keyCode, Slot hoveredSlot, Runnable parent, Function<Slot, ItemReference> containerRef) {
        if (hoveredSlot != null &&
                (ConfigScreen.isAirEditable() || hoveredSlot.getStack() != null && !hoveredSlot.getStack().isEmpty())) {
            ItemReference ref;
            if (hoveredSlot.inventory == MiscUtil.client.player.getInventory()) {
                ref = new InventoryItemReference(hoveredSlot.getIndex());
                if (parent != null) {
                    ((InventoryItemReference) ref).setParent(parent);
                }
            } else {
                ref = containerRef.apply(hoveredSlot);
            }
            return handleKeybind(keyCode, hoveredSlot.getStack(), ref);
        }
        return false;
    }

    public static boolean handleKeybind(int keyCode, ItemStack item, ItemReference ref) {
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (item == null || item.isEmpty()) {
                return false;
            }
            ref.saveItem(ItemStack.EMPTY);
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_SPACE) {
            return false;
        }

        boolean notAir = item != null && !item.isEmpty();
        if (hasControlDown()) {
            if (notAir && ContainerIO.isContainer(item)) {
                ContainerScreen.show(ref);
            }
        } else if (hasShiftDown()) {
            if (notAir) {
                MiscUtil.client.setScreen(new LocalFactoryScreen<>(ref));
            }
        } else {
            MiscUtil.client.setScreen(new NBTEditorScreen<>(ref));
        }

        return true;
    }

    public ServerInventoryManager getServerInventoryManager() {
        return serverInv;
    }

    @Override
    protected void init() {
        super.init();
        serverInv = new ServerInventoryManager();
    }

    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        DrawableHelper.drawTexture(matrices, TEXTURE, x, y, 0, 0, backgroundWidth, handler.getRows() * 18 + 17);
        DrawableHelper.drawTexture(matrices, TEXTURE, x, y + handler.getRows() * 18 + 17, 0, 126, backgroundWidth, 96);
    }

    @Override
    protected final void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawBackground(DrawableHelper.getMatrices(context), delta, mouseX, mouseY);
    }

    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        getLockedSlotsInfo().renderLockedHighlights(matrices, handler, true, false, true);

        DrawableHelper.drawTextWithoutShadow(matrices, textRenderer, getRenderedTitle(), titleX, titleY, 4210752);
        DrawableHelper.drawTextWithoutShadow(matrices, textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 4210752);
    }

    @Override
    protected final void drawForeground(DrawContext context, int mouseX, int mouseY) {
        drawForeground(DrawableHelper.getMatrices(context), mouseX, mouseY);
    }

    protected Text getRenderedTitle() {
        return title;
    }

    @Override
    public void setInitialFocus(Element element) {
        MVMisc.setInitialFocus(this, element, super::setInitialFocus);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public final void tick() {
        super.tick();
    }

    @Override
    public void close() {
        M0DevToolsClient.CURSOR_MANAGER.closeRoot();
    }

    @Override
    public void removed() {
        serverInv = null;
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if (slot != null) {
            LockedSlotsInfo lockedSlotsInfo = getLockedSlotsInfo();
            if (lockedSlotsInfo.isBlocked(slot, button, actionType, false)) {
                if (lockedSlotsInfo.isCopyLockedItem() && slot.inventory != client.player.getInventory()) {
                    switch (actionType) {
                        case PICKUP, PICKUP_ALL -> {
                            ItemStack item = slot.getStack();
                            if (item.isEmpty()) {
                                break;
                            }
                            if (!handler.getCursorStack().isEmpty() &&
                                    !ItemStack.areItemsAndComponentsEqual(item, handler.getCursorStack())) {
                                handler.setCursorStack(ItemStack.EMPTY);
                            }
                            ItemStack cursor = handler.getCursorStack();
                            if (!cursor.isEmpty()) {
                                cursor.setCount(Math.min(cursor.getMaxCount(), cursor.getCount() + item.getCount()));
                                handler.setCursorStack(cursor);
                            } else {
                                handler.setCursorStack(item.copy());
                            }
                            serverInv.updateServer();
                        }
                        case CLONE -> {
                            ItemStack item = slot.getStack();
                            if (item.isEmpty()) {
                                break;
                            }
                            if (!handler.getCursorStack().isEmpty()) {
                                break;
                            }
                            item = item.copy();
                            item.setCount(item.getMaxCount());
                            handler.setCursorStack(item);
                            serverInv.updateServer();
                        }
                        case QUICK_MOVE -> {
                            ItemStack prevItem = slot.getStack().copy();
                            LockableSlot.unlockDuring(() -> handler.onSlotClick(slot.id, button, actionType, MiscUtil.client.player));
                            slot.setStackNoCallbacks(prevItem);
                            serverInv.updateServer();
                        }
                        case THROW -> {
                            ItemStack item = slot.getStack();
                            if (button == 0) {
                                item = item.copy();
                                item.setCount(1);
                            }
                            MiscUtil.dropCreativeStack(item);
                        }
                        case SWAP -> {
                        }
                        case QUICK_CRAFT -> throw new IllegalArgumentException("Invalid SlotActionType: " + actionType);
                    }
                }
                return;
            }
        }

        if (!(slot != null && allowEnchantmentCombine() && Screen.hasControlDown() && tryCombineEnchantments(slot, actionType))) {
            handler.onSlotClick(slot == null ? slotId : slot.id, button, actionType, MiscUtil.client.player);
        }

        serverInv.updateServer();
        onChange();
    }

    private boolean tryCombineEnchantments(Slot slot, SlotActionType actionType) {
        if (actionType == SlotActionType.PICKUP && slot != null) {
            ItemStack cursor = handler.getCursorStack();
            ItemStack item = slot.getStack();
            if (cursor == null || cursor.isEmpty() || item == null || item.isEmpty()) {
                return false;
            }
            if (cursor.getItem() == Items.ENCHANTED_BOOK || item.getItem() == Items.ENCHANTED_BOOK) {
                if (cursor.getItem() != Items.ENCHANTED_BOOK) { // Make sure the cursor is an enchanted book
                    ItemStack temp = cursor;
                    cursor = item;
                    item = temp;
                }

                Enchants enchants = ItemTagReferences.ENCHANTMENTS.get(item);
                enchants.addEnchants(ItemTagReferences.ENCHANTMENTS.get(cursor).enchants());
                ItemTagReferences.ENCHANTMENTS.set(item, enchants);

                slot.setStackNoCallbacks(item);
                handler.setCursorStack(ItemStack.EMPTY);
                return true;
            }
        }

        return false;
    }

    public boolean allowEnchantmentCombine() {
        return false;
    }

    public LockedSlotsInfo getLockedSlotsInfo() {
        return LockedSlotsInfo.NONE;
    }

    public void onChange() {
        // Will be overridden
    }

}
