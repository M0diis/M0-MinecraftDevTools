package me.m0dii.nbteditor.nbtreferences.itemreferences;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.m0dii.M0DevToolsClient;
import me.m0dii.nbteditor.localnbt.LocalItem;
import me.m0dii.nbteditor.localnbt.LocalItemStack;
import me.m0dii.nbteditor.localnbt.LocalNBT;
import me.m0dii.nbteditor.multiversion.MVRegistry;
import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.multiversion.nbt.NBTManagers;
import me.m0dii.nbteditor.nbtreferences.NBTReference;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.lang.reflect.Proxy;
import java.util.function.Predicate;

public interface ItemReference extends NBTReference<LocalItem> {
    static ItemReference getHeldItem(Predicate<ItemStack> isAllowed, Text failText) throws CommandSyntaxException {
        ItemStack item = MiscUtil.client.player.getMainHandStack();
        Hand hand = Hand.MAIN_HAND;

        if (item == null || item.isEmpty() || !isAllowed.test(item)) {
            item = MiscUtil.client.player.getOffHandStack();
            hand = Hand.OFF_HAND;
        }

        if (item == null || item.isEmpty() || !isAllowed.test(item)) {
            throw new SimpleCommandExceptionType(failText).create();
        }

        return new HandItemReference(hand);
    }

    static ItemReference getHeldItem() throws CommandSyntaxException {
        return getHeldItem(item -> true, TextInst.translatable("nbteditor.no_hand.no_item.to_edit"));
    }

    static ItemReference getHeldItemAirable() {
        try {
            return getHeldItem();
        } catch (CommandSyntaxException e) {
            return new HandItemReference(Hand.MAIN_HAND);
        }
    }

    static ItemReference getHeldAir() throws CommandSyntaxException {
        if (MiscUtil.client.player.getMainHandStack().isEmpty()) {
            return new HandItemReference(Hand.MAIN_HAND);
        }

        if (MiscUtil.client.player.getOffHandStack().isEmpty()) {
            return new HandItemReference(Hand.OFF_HAND);
        }

        throw new SimpleCommandExceptionType(TextInst.translatable("nbteditor.no_hand.all_item")).create();
    }

    static ItemReference getContainerItem(HandledScreen<?> screen, Slot slot) {
        if (slot.inventory == MiscUtil.client.player.getInventory()) {
            return new InventoryItemReference(slot.getIndex()).setParent(
                    () -> M0DevToolsClient.CURSOR_MANAGER.showBranch(screen));
        }
        return new ServerItemReference(screen, slot.id);
    }

    @SuppressWarnings("unchecked")
    static <T extends LocalNBT> NBTReference<T> toItemStackRef(NBTReference<T> ref) {
        if (ref instanceof ItemReference itemRef) {
            return (NBTReference<T>) itemRef.toStackRef();
        }
        return ref;
    }

    /**
     * @see #toPartsRef() for deprecation details
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    static <T extends LocalNBT> NBTReference<T> toItemPartsRef(NBTReference<T> ref) {
        if (ref instanceof ItemReference itemRef) {
            return (NBTReference<T>) itemRef.toPartsRef();
        }
        return ref;
    }

    default ItemReference toStackRef() {
        return this;
    }

    /**
     * Make sure to call {@link #toStackRef()} before passing this to any code not designed for a parts ref!<br>
     * Also, make sure to never call {@link LocalItem#getEditableItem()}!
     */
    @Deprecated
    default ItemReference toPartsRef() {
        ItemReference stackRef = this;
        return (ItemReference) Proxy.newProxyInstance(ItemReference.class.getClassLoader(),
                new Class<?>[]{ItemReference.class}, (obj, method, args) -> {
                    if (method.getName().equals("toStackRef")) {
                        return stackRef;
                    }
                    if (method.getName().equals("toPartsRef")) {
                        return obj;
                    }

                    Object output = method.invoke(stackRef, args);
                    if (output instanceof LocalItem localItem) {
                        return localItem.toParts();
                    }
                    return output;
                });
    }

    @Override
    default LocalItem getLocalNBT() {
        return new LocalItemStack(getItem());
    }

    @Override
    default void saveLocalNBT(LocalItem nbt, Runnable onFinished) {
        saveItem(nbt.getReadableItem(), onFinished);
    }

    ItemStack getItem();

    void saveItem(ItemStack toSave, Runnable onFinished);

    default void saveItem(ItemStack toSave, Text msg) {
        saveItem(toSave, () -> MiscUtil.client.player.sendMessage(msg, false));
    }

    default void saveItem(ItemStack toSave) {
        saveItem(toSave, () -> {
        });
    }

    boolean isLocked();

    boolean isLockable();

    /**
     * Prevents a slot from being clicked or swapped while open in a container screen
     *
     * @return The slot to block (format: inv) or -1 if no slot should be blocked
     */
    int getBlockedSlot();

    @Override
    default Identifier getId() {
        return MVRegistry.ITEM.getId(getItem().getItem());
    }

    @Override
    default NbtCompound getNBT() {
        NbtCompound nbt = NBTManagers.ITEM.getNbt(getItem());

        if (nbt != null) {
            return nbt;
        }

        return new NbtCompound();
    }

    @Override
    default void saveNBT(Identifier id, NbtCompound toSave, Runnable onFinished) {
        ItemStack item = getItem();
        if (!MVRegistry.ITEM.getId(item.getItem()).equals(id)) {
            item = MiscUtil.setType(MVRegistry.ITEM.get(id), item);
        }
        item.manager$setNbt(toSave);
        saveItem(item, onFinished);
    }

    @Override
    void showParent();
}
