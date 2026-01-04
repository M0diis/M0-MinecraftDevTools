package me.m0dii.modules;

import me.m0dii.modules.commandhistory.CommandHistoryModule;
import me.m0dii.modules.entityradar.EntityRadarModule;
import me.m0dii.modules.freecam.FreecamModule;
import me.m0dii.modules.instabreak.InstaBreakModule;
import me.m0dii.modules.inventorymove.InventoryMoveModule;
import me.m0dii.modules.macros.gui.MacroKeybindOverlayModule;
import me.m0dii.modules.macros.gui.PendingMacrosOverlayModule;
import me.m0dii.modules.nbttooltip.NBTTooltipModule;
import me.m0dii.modules.overlays.LightLevelOverlayModule;
import me.m0dii.modules.overlays.RedstonePowerOverlayModule;
import me.m0dii.modules.overlays.SlimeChunkOverlayModule;
import me.m0dii.modules.nbthud.NBTInfoHudOverlayModule;
import me.m0dii.modules.xray.XrayModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class ModulesScreen extends Screen {
    private final Screen parent;

    public ModulesScreen(Screen parent) {
        super(Text.literal("M0 Dev Tools - Modules"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridWidget grid = new GridWidget();
        grid.getMainPositioner().margin(4);

        GridWidget.Adder adder = grid.createAdder(3);

        adder.add(sectionHeader("Overlays"), 3);

        adder.add(RedstonePowerOverlayModule.INSTANCE.getToggleButton());
        adder.add(SlimeChunkOverlayModule.INSTANCE.getToggleButton());
        adder.add(LightLevelOverlayModule.INSTANCE.getToggleButton());

        adder.add(sectionHeader("HUD"), 3);
        adder.add(MacroKeybindOverlayModule.INSTANCE.getToggleButton());
        adder.add(PendingMacrosOverlayModule.INSTANCE.getToggleButton());
        adder.add(NBTInfoHudOverlayModule.INSTANCE.getToggleButton());
        adder.add(EntityRadarModule.INSTANCE.getToggleButton());

        adder.add(sectionHeader("Utilities"), 3);
        adder.add(XrayModule.INSTANCE.getToggleButton());
        adder.add(CommandHistoryModule.INSTANCE.getToggleButton());
        adder.add(FreecamModule.INSTANCE.getToggleButton());
        adder.add(InstaBreakModule.INSTANCE.getToggleButton());
        adder.add(NBTTooltipModule.INSTANCE.getToggleButton());
        adder.add(InventoryMoveModule.INSTANCE.getToggleButton());

        adder.add(sectionHeader(""), 3);

        adder.add(new TextWidget(Text.literal(""), this.textRenderer), 1);
        adder.add(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .width(150)
                .build(), 1);

        grid.refreshPositions();
        SimplePositioningWidget.setPos(grid, 0, this.height / 4, this.width, this.height, 0.5f, 0.0f);
        grid.forEachChild(this::addDrawableChild);
    }

    private TextWidget sectionHeader(String title) {
        TextWidget w = new TextWidget(Text.literal(title), this.textRenderer);
        w.setWidth(150 * 3 + 16);
        return w;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
