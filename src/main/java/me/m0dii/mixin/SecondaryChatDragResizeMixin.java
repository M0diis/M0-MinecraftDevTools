package me.m0dii.mixin;

import me.m0dii.utils.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class SecondaryChatDragResizeMixin {

    @Unique
    private boolean m0devtools$dragging = false;
    @Unique
    private boolean m0devtools$resizing = false;
    @Unique
    private int m0devtools$grabDx;
    @Unique
    private int m0devtools$grabDy;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void m0devtools$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.secondaryChatEnabled || !ModConfig.secondaryChatShowOverlay) {
            return;
        }
        if (button != 0) {
            return;
        }

        int x = ModConfig.secondaryChatX;
        int y = ModConfig.secondaryChatY;
        int w = Math.max(50, ModConfig.secondaryChatWidth);
        int h = Math.max(30, ModConfig.secondaryChatHeight);

        // Resize handle: bottom-right 12x12 pixels (no modifier)
        int hx = x + w - 12;
        int hy = y + h - 12;
        if (inside(mouseX, mouseY, hx, hy, 12, 12)) {
            m0devtools$resizing = true;
            m0devtools$dragging = false;
            // Store offset from lower-right corner
            m0devtools$grabDx = (int) mouseX - (x + w);
            m0devtools$grabDy = (int) mouseY - (y + h);
            cir.setReturnValue(true);
            return;
        }

        // Hold SHIFT + drag anywhere inside to move
        if (inside(mouseX, mouseY, x, y, w, h) && shiftDown()) {
            m0devtools$dragging = true;
            m0devtools$resizing = false;
            m0devtools$grabDx = (int) mouseX - x;
            m0devtools$grabDy = (int) mouseY - y;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void m0devtools$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.secondaryChatEnabled || !ModConfig.secondaryChatShowOverlay) {
            return;
        }
        if (button != 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        if (m0devtools$dragging) {
            ModConfig.secondaryChatX = (int) mouseX - m0devtools$grabDx;
            ModConfig.secondaryChatY = (int) mouseY - m0devtools$grabDy;
            cir.setReturnValue(true);
        } else if (m0devtools$resizing) {
            int newW = (int) mouseX - ModConfig.secondaryChatX - m0devtools$grabDx;
            int newH = (int) mouseY - ModConfig.secondaryChatY - m0devtools$grabDy;
            ModConfig.secondaryChatWidth = Math.max(50, newW);
            ModConfig.secondaryChatHeight = Math.max(30, newH);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void m0devtools$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) {
            return;
        }
        m0devtools$dragging = false;
        m0devtools$resizing = false;
    }

    @Unique
    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Unique
    private static boolean shiftDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return false;
        }
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
