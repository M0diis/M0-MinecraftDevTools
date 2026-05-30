package me.m0dii.modules.scripting.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.m0dii.modules.scripting.GroovyScriptManager;
import me.m0dii.modules.scripting.KotlinScriptManager;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

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
        return OwoUIAdapter.create(this, (sizing, sizing1) -> UIContainers.grid(sizing, sizing, 5, 5));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void build(GridLayout rootComponent) {
        rootComponent.padding(Insets.of(10)).surface(Surface.VANILLA_TRANSLUCENT);

        fileNameBox = UIComponents.textBox(Sizing.fixed(200));
        rootComponent.child(fileNameBox, 0, 0);

        scriptBox = UIComponents.textArea(Sizing.fill(100), Sizing.fill(30));
        configureLargeTextLimit(scriptBox, 1_000_000);
        rootComponent.child(scriptBox, 1, 0);

        outputBox = UIComponents.textArea(Sizing.fill(100), Sizing.fixed(60));
        configureLargeTextLimit(outputBox, 200_000);
        rootComponent.child(outputBox, 3, 0);

        rootComponent.child(UIComponents.button(Text.literal("Run"), button -> runScript())
                .horizontalSizing(Sizing.fixed(60)), 4, 0);
        rootComponent.child(UIComponents.button(Text.literal("Save"), button -> saveScript())
                .horizontalSizing(Sizing.fixed(60)), 4, 1);
        rootComponent.child(UIComponents.button(Text.literal("Load"), button -> loadScript())
                .horizontalSizing(Sizing.fixed(60)), 4, 2);
        rootComponent.child(UIComponents.button(Text.literal("Docs"), button -> showDocs())
                .horizontalSizing(Sizing.fixed(60)), 4, 3);
        rootComponent.child(UIComponents.button(Text.literal("Scripts"), button -> showScripts())
                .horizontalSizing(Sizing.fixed(60)), 4, 4);

    }

    private void runScript() {
        try {
            String script = normalizeNewlines(scriptBox.getText());
            String fileName = fileNameBox.getText() == null ? "" : fileNameBox.getText().trim().toLowerCase(Locale.ROOT);

            Object result;
            if (fileName.endsWith(GROOVY_EXT)) {
                result = new GroovyScriptManager().runScript(script);
            } else {
                // Default to Kotlin when extension is missing; users often paste .kts snippets first.
                result = new KotlinScriptManager().runScript(script);
            }
            outputBox.setText(String.valueOf(result));
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
            String fileName = (name.endsWith(GROOVY_EXT) || name.endsWith(KOTLIN_EXT)) ? name : name + KOTLIN_EXT;
            Path file = SCRIPTS_DIR.resolve(fileName);
            Files.writeString(file, normalizeNewlines(scriptBox.getText()));
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

            String fileName = (name.endsWith(GROOVY_EXT) || name.endsWith(KOTLIN_EXT)) ? name : name + KOTLIN_EXT;
            Path file = SCRIPTS_DIR.resolve(fileName);

            if (!Files.exists(file)) {
                outputBox.setText("File not found!");
                return;
            }
            String content = normalizeNewlines(Files.readString(file));
            scriptBox.setText(content);
            fileNameBox.setText(fileName);
            outputBox.setText("Loaded " + file.getFileName());
        } catch (IOException e) {
            outputBox.setText("Load error: " + e.getMessage());
        }
    }

    private static String normalizeNewlines(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void configureLargeTextLimit(TextAreaComponent area, int limit) {
        if (area == null) {
            return;
        }
        // Owo API names differ by version; try common setters reflectively.
        for (String methodName : new String[]{"setMaxLength", "maxLength", "textLimit", "setTextLimit"}) {
            try {
                Method method = area.getClass().getMethod(methodName, int.class);
                method.invoke(area, limit);
                return;
            } catch (Exception ignored) {
                // Try next known method name.
            }
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
