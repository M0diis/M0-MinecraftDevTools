package me.m0dii.nbteditor.screens.configurable;

import me.m0dii.nbteditor.multiversion.MVDrawable;
import me.m0dii.nbteditor.multiversion.MVElement;

import java.util.WeakHashMap;

public interface Configurable<T extends Configurable<T>> extends MVDrawable, MVElement {
    int PADDING = 8;
    WeakHashMap<Configurable<?>, ConfigPath> PARENTS = new WeakHashMap<>();

    boolean isValueValid();

    int getSpacingWidth();

    int getSpacingHeight();

    default int getRenderWidth() {
        return getSpacingWidth();
    }

    default int getRenderHeight() {
        return getSpacingHeight();
    }

    T clone(boolean defaults);

    default ConfigPath getParent() {
        return PARENTS.get(this);
    }

    default void setParent(ConfigPath parent) {
        PARENTS.put(this, parent);
    }

    @Override
    default boolean isMouseOver(double mouseX, double mouseY) {
        return mouseY >= 0 && mouseY <= getSpacingHeight();
    }
}
