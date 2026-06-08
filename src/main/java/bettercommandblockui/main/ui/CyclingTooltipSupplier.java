package bettercommandblockui.main.ui;

import lombok.Getter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

public class CyclingTooltipSupplier {
    private final Screen screen;
    private final Text[] tooltips;
    @Getter
    private int currentIndex;

    public CyclingTooltipSupplier(Screen screen, int initialIndex, Text[] tooltips) {
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
        return Tooltip.of(tooltips[currentIndex]);
    }

}
