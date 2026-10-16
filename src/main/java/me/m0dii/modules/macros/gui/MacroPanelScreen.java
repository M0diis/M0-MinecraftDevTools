package me.m0dii.modules.macros.gui;


import me.m0dii.gui.theme.ThemedScreen;
import me.m0dii.modules.macros.CommandMacros;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MacroPanelScreen extends Screen implements ThemedScreen {

    private final Screen parent;

    public MacroPanelScreen(Screen parent) {
        super(Component.literal("Macro Panel"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new MacroPanelScreen(parent);
    }

    @Override
    protected void init() {
        super.init();

        MacroPanelDataHandler.PanelConfig cfg = MacroPanelDataHandler.getConfig();

        this.addRenderableWidget(Button.builder(Component.literal("Edit"), btn -> {
                    if (this.minecraft != null) {
                        this.minecraft.gui.setScreen(MacroPanelEditorScreen.create(this));
                    }
                }).bounds(this.width - 52, 8, 40, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
                .bounds(this.width - 98, 8, 44, 18)
                .build());

        if (!cfg.enabled) {
            this.addRenderableWidget(Button.builder(Component.literal("Panel disabled (enable in editor)"), btn -> {
                        if (this.minecraft != null) {
                            this.minecraft.gui.setScreen(MacroPanelEditorScreen.create(this));
                        }
                    }).bounds(this.width / 2 - 90, this.height / 2 - 10, 180, 20)
                    .build());
            return;
        }

        for (MacroPanelDataHandler.PanelButton b : cfg.buttons) {
            int x = Math.clamp(b.x, 0, Math.max(0, this.width - b.width));
            int y = Math.clamp(b.y, 20, Math.max(20, this.height - b.height));

            this.addRenderableWidget(Button.builder(Component.literal(b.label), btn -> onPanelButtonPressed(b.macroId, b.label))
                    .bounds(x, y, b.width, b.height)
                    .build());
        }

        if (cfg.buttons.isEmpty()) {
            this.addRenderableWidget(Button.builder(Component.literal("Add buttons in editor"), btn -> {
                        if (this.minecraft != null) {
                            this.minecraft.gui.setScreen(MacroPanelEditorScreen.create(this));
                        }
                    }).bounds(this.width / 2 - 75, this.height / 2 - 10, 150, 20)
                    .build());
        }
    }

    private void onPanelButtonPressed(String macroId, String label) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        if (macroId == null || macroId.isBlank()) {
            this.minecraft.player.sendSystemMessage(Component.literal("Macro button '" + label + "' has no macro id."));
            return;
        }

        boolean ok = CommandMacros.runMacroById(macroId);
        if (!ok) {
            this.minecraft.player.sendSystemMessage(Component.literal("Unknown macro id: " + macroId).withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x90101010);
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.extractBackground(context, mouseX, mouseY, delta);

        MacroPanelDataHandler.PanelConfig cfg = MacroPanelDataHandler.getConfig();
        context.text(this.font, cfg.title, 12, 12, 0xFFFFFF);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

