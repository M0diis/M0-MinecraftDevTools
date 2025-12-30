package me.m0dii.modules.scripting.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ScriptDocsScreen extends Screen {
    private final Screen parent;

    private static final String DOCS = """
            Script Editor Help:
            
            - Write Groovy/Kotlin scripts in the main editor area.
            - Use Ctrl+V/C/X/A for paste/copy/cut/select all.
            - Use the mouse to move the cursor and select text.
            - Save: Save the current script to a file.
            - Load: Load a script from the file name.
            - Scripts: Open the script manager for saved scripts.
            - Output: Shows script results/errors.
            - File name: Set or view the current script's filename. (include .groovy or .kts)
            - Output is shown in the field at the bottom.
            
            Scripting Tips:
            - You can use Minecraft and mod classes in your scripts.
            - Scripts are saved in config/m0-dev-tools/scripts.
            
            Available Variables:
            - client: MinecraftClient instance
            - source: Command source or player entity
            - player: Player entity
            - world: Current world instance
            - options: Game options/settings
            - server: Integrated server instance (if applicable)
            """;

    public ScriptDocsScreen(Screen parent) {
        super(Text.literal("Groovy Script Editor Documentation"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        ButtonWidget backButton = ButtonWidget.builder(Text.literal("Back"),
                        btn -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(this.width / 2 - 30, this.height - 40, 60, 20).build();

        this.addDrawableChild(backButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        int x = 40;
        int y = 40;

        int lineHeight = 14;
        String[] lines = DOCS.split("\n");
        for (int i = 0; i < lines.length; i++) {
            context.drawText(this.textRenderer, lines[i], x, y + i * lineHeight, 0xFFFFFF, false);
        }
    }
}

