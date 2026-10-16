package me.m0dii.gui.local;

import me.m0dii.gui.theme.ThemeDraw;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class UiForms {
    private UiForms() {
    }

    public static EditBox addLabeledField(Screen screen,
                                                  Font textRenderer,
                                                  int x,
                                                  int y,
                                                  int labelWidth,
                                                  String label,
                                                  int fieldWidth,
                                                  String value) {
        EditBox field = new EditBox(textRenderer, x + labelWidth, y, fieldWidth, 20, Component.empty());
        field.setMaxLength(96);
        field.setValue(value == null ? "" : value);
        screen.addRenderableWidget(field);
        return field;
    }

    public static void drawPanel(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        ThemeDraw.card(context, x, y, w, h);
    }

}

