package me.m0dii.nbteditor.screens;

import me.m0dii.nbteditor.multiversion.TextInst;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class WidgetScreen extends OverlaySupportingScreen {

    private Screen parent;

    private <T extends Drawable & Element & Selectable> WidgetScreen(Text title, T widget, boolean restoreParent) {
        super(title);
        setOverlay(widget);

        if (restoreParent) {
            parent = MiscUtil.client.currentScreen;
        }
    }

    public static <T extends Drawable & Element & Selectable> T setOverlayOrScreen(T overlay, double z, boolean restoreParent) {
        if (MiscUtil.client.currentScreen instanceof OverlaySupportingScreen screen) {
            screen.setOverlay(overlay, z);
        } else {
            MiscUtil.client.setScreen(new WidgetScreen(TextInst.of(overlay.getClass().getName()), overlay, restoreParent));
        }
        return overlay;
    }

    @Override
    public <T extends Drawable & Element> T setOverlay(T overlay, double z) {
        if (overlay == null) {
            MiscUtil.client.setScreen(parent);
        } else {
            parent = null;
        }
        return super.setOverlay(overlay, z);
    }

    @Override
    public <T extends Screen> T setOverlayScreen(T overlay, double z) {
        if (overlay == null) {
            MiscUtil.client.setScreen(parent);
        } else {
            parent = null;
        }

        return super.setOverlayScreen(overlay, z);
    }

    @Override
    protected void renderMain(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);
        super.renderMain(matrices, mouseX, mouseY, delta);
    }

}
