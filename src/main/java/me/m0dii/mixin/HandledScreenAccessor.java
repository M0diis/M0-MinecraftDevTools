package me.m0dii.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Invoker("getSlotAt")
    Slot m0dev$invokeGetSlotAt(double mouseX, double mouseY);

    @Invoker("onMouseClick")
    void m0dev$invokeOnMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Accessor("cursorDragging")
    boolean m0dev$isCursorDragging();

    @Accessor("cursorDragging")
    void m0dev$setCursorDragging(boolean value);

    @Accessor("heldButtonType")
    int m0dev$getHeldButtonType();

    @Accessor("cancelNextRelease")
    void m0dev$setCancelNextRelease(boolean value);

    @Accessor("cursorDragSlots")
    Set<Slot> m0dev$getCursorDragSlots();
}
