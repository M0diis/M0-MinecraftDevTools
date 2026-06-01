package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.CommandMacros;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MacroPanelScreen extends Screen {

    private final Screen parent;

    public MacroPanelScreen(Screen parent) {
        super(Text.literal("Macro Panel"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new MacroPanelScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        MacroPanelDataHandler.PanelConfig cfg = MacroPanelDataHandler.getConfig();

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit"), btn -> {
                    if (this.client != null) {
                        this.client.setScreen(MacroPanelEditorScreen.create(this));
                    }
                }).dimensions(this.width - 52, 8, 40, 18)
                .build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(this.width - 98, 8, 44, 18)
                .build());

        if (!cfg.enabled) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Panel disabled (enable in editor)"), btn -> {
                        if (this.client != null) {
                            this.client.setScreen(MacroPanelEditorScreen.create(this));
                        }
                    }).dimensions(this.width / 2 - 90, this.height / 2 - 10, 180, 20)
                    .build());
            return;
        }

        for (MacroPanelDataHandler.PanelButton b : cfg.buttons) {
            int x = Math.clamp(b.x, 0, Math.max(0, this.width - b.width));
            int y = Math.clamp(b.y, 20, Math.max(20, this.height - b.height));

            this.addDrawableChild(ButtonWidget.builder(Text.literal(b.label), btn -> onPanelButtonPressed(b.macroId, b.label))
                    .dimensions(x, y, b.width, b.height)
                    .build());
        }

        if (cfg.buttons.isEmpty()) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Add buttons in editor"), btn -> {
                        if (this.client != null) {
                            this.client.setScreen(MacroPanelEditorScreen.create(this));
                        }
                    }).dimensions(this.width / 2 - 75, this.height / 2 - 10, 150, 20)
                    .build());
        }
    }

    private void onPanelButtonPressed(String macroId, String label) {
        if (this.client == null || this.client.player == null) {
            return;
        }

        if (macroId == null || macroId.isBlank()) {
            this.client.player.sendMessage(Text.literal("Macro button '" + label + "' has no macro id."), false);
            return;
        }

        boolean ok = CommandMacros.runMacroById(macroId);
        if (!ok) {
            this.client.player.sendMessage(Text.literal("Unknown macro id: " + macroId).formatted(Formatting.RED), false);
        }
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90101010);
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        MacroPanelDataHandler.PanelConfig cfg = MacroPanelDataHandler.getConfig();
        context.drawTextWithShadow(this.textRenderer, cfg.title, 12, 12, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

