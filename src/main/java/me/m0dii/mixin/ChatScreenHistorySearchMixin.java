package me.m0dii.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import me.m0dii.modules.commandhistory.ChatHistorySearch;
import me.m0dii.modules.commandhistory.CommandHistoryManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ctrl+R / Ctrl+Shift+R in the chat box searches command history, like a terminal's reverse search. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenHistorySearchMixin extends Screen {
    @Shadow
    protected EditBox input;

    @Unique
    private final ChatHistorySearch m0dev$historySearch = new ChatHistorySearch();

    protected ChatScreenHistorySearchMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void m0dev$reverseSearch(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.input() != InputConstants.KEY_R || this.input == null) {
            return;
        }
        boolean ctrl = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
        if (!ctrl) {
            return;
        }
        boolean shift = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
        String current = this.input.getValue();
        String found = shift
                ? m0dev$historySearch.previous(CommandHistoryManager.getHistory(), current)
                : m0dev$historySearch.next(CommandHistoryManager.getHistory(), current);
        if (found != null) {
            this.input.setValue(found);
            this.input.moveCursorTo(found.length(), false);
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void m0dev$drawSearchStatus(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.input == null) {
            return;
        }
        String current = this.input.getValue();
        boolean showing = m0dev$historySearch.isShowing(current);
        if (!showing && !m0dev$historySearch.hadNoMatch(current)) {
            return;
        }
        String status = m0dev$historySearch.status();
        int y = this.height - 26;
        context.fill(2, y - 2, 6 + this.font.width(status), y + 10, 0xC0000000);
        context.text(this.font, status, 4, y, showing ? 0xFF9FC8FF : 0xFFFF9D9D, false);
    }
}
