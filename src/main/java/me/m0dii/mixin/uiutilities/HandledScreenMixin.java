package me.m0dii.mixin.uiutilities;

import me.m0dii.modules.uiutilities.UiUtilitiesModule;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    private HandledScreenMixin() {
        super(null);
    }

    @Shadow
    protected abstract boolean handleHotbarKeyPressed(int keyCode, int scanCode);

    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Unique
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Unique
    private TextFieldWidget inputField;

    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {
        if (UiUtilitiesModule.INSTANCE.isEnabled()) {
            UiUtilitiesModule.INSTANCE.createUtilityButtons(mc, this);
            this.inputField = UiUtilitiesModule.createInputField(this.textRenderer, mc);
            this.addDrawableChild(this.inputField);
        }
    }

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    public void keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        } else if (MiscUtil.client.options.inventoryKey.matchesKey(keyCode, scanCode) && (this.inputField == null || !this.inputField.isSelected())) {
            // Crashes if address field does not exist
            this.close();
            cir.setReturnValue(true);
        } else {
            this.handleHotbarKeyPressed(keyCode, scanCode);
            if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
                if (mc.options.pickItemKey.matchesKey(keyCode, scanCode)) {
                    this.onMouseClick(this.focusedSlot, this.focusedSlot.id, 0, SlotActionType.CLONE);
                } else if (mc.options.dropKey.matchesKey(keyCode, scanCode)) {
                    this.onMouseClick(this.focusedSlot, this.focusedSlot.id, hasControlDown() ? 1 : 0, SlotActionType.THROW);
                }
            }

            cir.setReturnValue(true);
        }
    }

}
