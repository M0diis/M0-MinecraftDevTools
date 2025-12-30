package me.m0dii.nbteditor.screens.widgets;

import me.m0dii.nbteditor.multiversion.MVDrawable;
import me.m0dii.nbteditor.multiversion.MVElement;
import me.m0dii.nbteditor.screens.Tickable;
import me.m0dii.nbteditor.util.MiscUtil;
import net.minecraft.client.gui.screen.Screen;

public interface InitializableOverlay<T extends Screen> extends MVDrawable, MVElement, Tickable {
    void init(T parent, int width, int height);

    default void tick() {
    }

    @SuppressWarnings("unchecked")
    default void initUnchecked(Screen parent) {
        init((T) parent, MiscUtil.client.getWindow().getScaledWidth(), MiscUtil.client.getWindow().getScaledHeight());
    }
}
