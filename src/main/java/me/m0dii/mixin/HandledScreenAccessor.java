package me.m0dii.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Invoker("getHoveredSlot")
    Slot m0dev$invokeGetSlotAt(double mouseX, double mouseY);

    @Invoker("slotClicked")
    void m0dev$invokeOnMouseClick(Slot slot, int slotId, int button, ContainerInput actionType);

    @Accessor("isQuickCrafting")
    boolean m0dev$isCursorDragging();

    @Accessor("isQuickCrafting")
    void m0dev$setCursorDragging(boolean value);

    @Accessor("quickCraftingType")
    int m0dev$getHeldButtonType();

    @Accessor("skipNextRelease")
    void m0dev$setCancelNextRelease(boolean value);

    @Accessor("quickCraftSlots")
    Set<Slot> m0dev$getCursorDragSlots();
}
