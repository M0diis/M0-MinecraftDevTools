package bettercommandblockui.main.ui;

import lombok.Getter;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CyclingTooltipSupplier {
    private final Screen screen;
    private final Component[] tooltips;
    @Getter
    private int currentIndex;

    public CyclingTooltipSupplier(Screen screen, int initialIndex, Component[] tooltips) {
        this.screen = screen;
        this.tooltips = tooltips;
        this.currentIndex = initialIndex;
    }

    public void incrementIndex() {
        currentIndex = (currentIndex + 1) % tooltips.length;
    }

    public void setIndex(int index) {
        currentIndex = index;
    }

    public Tooltip getTooltip() {
        return Tooltip.create(tooltips[currentIndex]);
    }

}
