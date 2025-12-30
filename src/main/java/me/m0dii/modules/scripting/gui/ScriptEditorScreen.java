package me.m0dii.modules.scripting.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.m0dii.modules.scripting.GroovyScriptManager;
import me.m0dii.modules.scripting.KotlinScriptManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ScriptEditorScreen extends BaseOwoScreen<GridLayout> {

    private static final Path SCRIPTS_DIR = Paths.get("config/m0-dev-tools/scripts");
    private static final String GROOVY_EXT = ".groovy";
    private static final String KOTLIN_EXT = ".kts";

    private TextBoxComponent fileNameBox;
    private TextAreaComponent outputBox;
    private TextAreaComponent scriptBox;

    @Override
    protected @NotNull OwoUIAdapter<GridLayout> createAdapter() {
        // 3 columns, 6 rows for all components
        return OwoUIAdapter.create(this, (sizing, sizing1) -> Containers.grid(sizing, sizing, 5, 5));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void build(GridLayout rootComponent) {
        rootComponent.padding(Insets.of(10)).surface(Surface.VANILLA_TRANSLUCENT);

        fileNameBox = Components.textBox(Sizing.fixed(200));
        rootComponent.child(fileNameBox, 0, 0);

        scriptBox = Components.textArea(Sizing.fill(100), Sizing.fill(30));
        rootComponent.child(scriptBox, 1, 0);

        outputBox = Components.textArea(Sizing.fill(100), Sizing.fixed(60));
        rootComponent.child(outputBox, 3, 0);

        rootComponent.child(Components.button(Text.literal("Run"), button -> runScript())
                .horizontalSizing(Sizing.fixed(60)), 4, 0);
        rootComponent.child(Components.button(Text.literal("Save"), button -> saveScript())
                .horizontalSizing(Sizing.fixed(60)), 4, 1);
        rootComponent.child(Components.button(Text.literal("Load"), button -> loadScript())
                .horizontalSizing(Sizing.fixed(60)), 4, 2);
        rootComponent.child(Components.button(Text.literal("Docs"), button -> showDocs())
                .horizontalSizing(Sizing.fixed(60)), 4, 3);
        rootComponent.child(Components.button(Text.literal("Scripts"), button -> showScripts())
                .horizontalSizing(Sizing.fixed(60)), 4, 4);

    }

    private void runScript() {
        try {
            // ddetermine script manager by extension
            if (fileNameBox.getText().trim().endsWith(GROOVY_EXT)) {
                GroovyScriptManager manager = new GroovyScriptManager();
                Object result = manager.runScript(scriptBox.getText());
                outputBox.setText(String.valueOf(result));
            }

            if (fileNameBox.getText().trim().endsWith(KOTLIN_EXT)) {
                KotlinScriptManager manager = new KotlinScriptManager();
                Object result = manager.runScript(scriptBox.getText());
                outputBox.setText(String.valueOf(result));
            }
        } catch (Exception e) {
            outputBox.setText("Error: " + e.getMessage());
        }
    }

    protected void saveScript() {
        try {
            if (!Files.exists(SCRIPTS_DIR)) {
                Files.createDirectories(SCRIPTS_DIR);
            }
            String name = fileNameBox.getText().trim();
            if (name.isEmpty()) {
                outputBox.setText("Enter file name!");
                return;
            }
            String fileName = (name.endsWith(GROOVY_EXT) || name.endsWith(KOTLIN_EXT)) ? name : name + GROOVY_EXT;
            Path file = SCRIPTS_DIR.resolve(fileName);
            Files.writeString(file, scriptBox.getText());
            outputBox.setText("Saved to " + file.getFileName());
        } catch (IOException e) {
            outputBox.setText("Save error: " + e.getMessage());
        }
    }

    protected void loadScript() {
        try {
            String name = fileNameBox.getText().trim();
            if (name.isEmpty()) {
                outputBox.setText("Enter file name!");
                return;
            }

            String fileName = (name.endsWith(GROOVY_EXT) || name.endsWith(KOTLIN_EXT)) ? name : name + GROOVY_EXT;
            Path file = SCRIPTS_DIR.resolve(fileName);

            if (!Files.exists(file)) {
                outputBox.setText("File not found!");
                return;
            }
            String content = Files.readString(file);
            scriptBox.setText(content);
            fileNameBox.setText(fileName);
            outputBox.setText("Loaded " + file.getFileName());
        } catch (IOException e) {
            outputBox.setText("Load error: " + e.getMessage());
        }
    }

    private void showScripts() {
        if (client != null) {
            this.client.setScreen(new SavedScriptsScreen(this));
        }
    }

    private void showDocs() {
        if (this.client != null) {
            this.client.setScreen(new ScriptDocsScreen(this));
        }
    }

    public void setScriptBoxText(String text) {
        if (scriptBox != null) {
            scriptBox.setText(text);
        }
    }

    public void setFileNameBoxText(String text) {
        fileNameBox.setText(text);
    }

    public void setOutputBoxText(String text) {
        if (outputBox != null) {
            outputBox.setText(text);
        }
    }
}
