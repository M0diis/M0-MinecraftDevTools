package me.m0dii.gui.local;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class UiForms {
    private UiForms() {
    }

    public static TextFieldWidget addLabeledField(Screen screen,
                                                  TextRenderer textRenderer,
                                                  int x,
                                                  int y,
                                                  int labelWidth,
                                                  String label,
                                                  int fieldWidth,
                                                  String value) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x + labelWidth, y, fieldWidth, 20, Text.empty());
        field.setMaxLength(96);
        field.setText(value == null ? "" : value);
        screen.addDrawableChild(field);
        return field;
    }

    public static void drawPanel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, UiTheme.PANEL);
        context.fill(x, y, x + w, y + 1, UiTheme.PANEL_BORDER);
        context.fill(x, y + h - 1, x + w, y + h, UiTheme.PANEL_BORDER);
        context.fill(x, y, x + 1, y + h, UiTheme.PANEL_BORDER);
        context.fill(x + w - 1, y, x + w, y + h, UiTheme.PANEL_BORDER);
    }

}

