package me.m0dii.nbteditor.screens.configurable;

import me.m0dii.nbteditor.multiversion.MVButtonWidget;
import me.m0dii.nbteditor.multiversion.MVTooltip;
import net.minecraft.text.Text;

public class ConfigButton extends MVButtonWidget implements ConfigPath {

    private final PressAction onPress;
    private final MVTooltip tooltip;

    public ConfigButton(int width, Text message, PressAction onPress, MVTooltip tooltip) {
        super(0, 0, width, 20, message, btn -> onPress.onPress((ConfigButton) btn), tooltip);
        this.onPress = onPress;
        this.tooltip = tooltip;
    }

    public ConfigButton(int width, Text message, PressAction onPress) {
        this(width, message, onPress, MVTooltip.EMPTY);
    }

    @Override
    public boolean isValueValid() {
        return true;
    }

    @Override
    public ConfigButton addValueListener(ConfigValueListener<ConfigValue<?, ?>> listener) {
        return this; // "Value" never changes
    }

    @Override
    public int getSpacingWidth() {
        return this.width;
    }

    @Override
    public int getSpacingHeight() {
        return this.height;
    }

    @Override
    public ConfigButton clone(boolean defaults) {
        return new ConfigButton(this.width, this.getMessage(), onPress, tooltip);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false; // Stop space from triggering the button
    }

    @Override
    public void tick() {
    }

    @FunctionalInterface
    public interface PressAction {
        void onPress(ConfigButton button);
    }

}
