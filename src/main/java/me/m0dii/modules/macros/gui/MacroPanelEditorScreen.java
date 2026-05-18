package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.MacroDataHandler;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class MacroPanelEditorScreen {

    private MacroPanelEditorScreen() {
    }

    private record ButtonState(
            String id,
            AtomicReference<String> label,
            AtomicReference<String> macroId,
            AtomicInteger x,
            AtomicInteger y,
            AtomicInteger width,
            AtomicInteger height,
            boolean[] delete
    ) {
    }

    public static Screen create(@Nullable Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Macro Panel Editor"))
                .setShouldListSmoothScroll(true)
                .setShouldTabsSmoothScroll(true)
                .solidBackground();

        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Panel"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        MacroPanelDataHandler.PanelConfig snapshot = MacroPanelDataHandler.getConfig();

        AtomicReference<String> panelTitle = new AtomicReference<>(snapshot.title);
        boolean[] panelEnabled = new boolean[]{snapshot.enabled};

        category.addEntry(eb.startStrField(Text.literal("Title"), snapshot.title)
                .setSaveConsumer(panelTitle::set)
                .setTooltip(Text.literal("Title shown in the panel screen"))
                .build());

        category.addEntry(eb.startBooleanToggle(Text.literal("Panel enabled"), snapshot.enabled)
                .setSaveConsumer(v -> panelEnabled[0] = v)
                .setTooltip(Text.literal("Disables all runtime panel buttons without deleting layout"))
                .build());

        Map<String, MacroDataHandler.MacroEntry> macros = MacroDataHandler.getAllMacros();
        String macroHelp = macros.isEmpty()
                ? "No macros created yet. Create macros in Macro Manager first."
                : "Available macro ids: " + String.join(", ", macros.keySet());

        category.addEntry(eb.startTextDescription(Text.literal(macroHelp)).build());

        List<ButtonState> states = new ArrayList<>();
        for (MacroPanelDataHandler.PanelButton button : snapshot.buttons) {
            AtomicReference<String> label = new AtomicReference<>(button.label);
            AtomicReference<String> macroId = new AtomicReference<>(button.macroId);
            AtomicInteger x = new AtomicInteger(button.x);
            AtomicInteger y = new AtomicInteger(button.y);
            AtomicInteger w = new AtomicInteger(button.width);
            AtomicInteger h = new AtomicInteger(button.height);
            boolean[] delete = new boolean[]{false};

            SubCategoryBuilder sub = eb.startSubCategory(Text.literal(button.label + " [" + button.id + "]"))
                    .setExpanded(false);

            sub.add(eb.startStrField(Text.literal("Label"), button.label)
                    .setSaveConsumer(label::set)
                    .build());
            sub.add(eb.startStrField(Text.literal("Macro Id"), button.macroId)
                    .setSaveConsumer(macroId::set)
                    .setTooltip(Text.literal("Exact macro id from Macro Manager"))
                    .build());
            sub.add(eb.startIntField(Text.literal("X"), button.x)
                    .setMin(0)
                    .setMax(6000)
                    .setSaveConsumer(x::set)
                    .build());
            sub.add(eb.startIntField(Text.literal("Y"), button.y)
                    .setMin(0)
                    .setMax(6000)
                    .setSaveConsumer(y::set)
                    .build());
            sub.add(eb.startIntField(Text.literal("Width"), button.width)
                    .setMin(50)
                    .setMax(240)
                    .setSaveConsumer(w::set)
                    .build());
            sub.add(eb.startIntField(Text.literal("Height"), button.height)
                    .setMin(18)
                    .setMax(40)
                    .setSaveConsumer(h::set)
                    .build());
            sub.add(eb.startBooleanToggle(Text.literal("Delete"), false)
                    .setSaveConsumer(v -> delete[0] = v)
                    .build());

            category.addEntry(sub.build());
            states.add(new ButtonState(button.id, label, macroId, x, y, w, h, delete));
        }

        AtomicReference<String> newLabel = new AtomicReference<>("New Button");
        AtomicReference<String> newMacroId = new AtomicReference<>("");
        AtomicInteger newX = new AtomicInteger(20);
        AtomicInteger newY = new AtomicInteger(40);
        AtomicInteger newW = new AtomicInteger(110);
        AtomicInteger newH = new AtomicInteger(20);
        boolean[] createNew = new boolean[]{false};

        SubCategoryBuilder addSub = eb.startSubCategory(Text.literal("Add Button"))
                .setExpanded(false);
        addSub.add(eb.startBooleanToggle(Text.literal("Create on save"), false)
                .setSaveConsumer(v -> createNew[0] = v)
                .build());
        addSub.add(eb.startStrField(Text.literal("Label"), "New Button")
                .setSaveConsumer(newLabel::set)
                .build());
        addSub.add(eb.startStrField(Text.literal("Macro Id"), "")
                .setSaveConsumer(newMacroId::set)
                .setTooltip(Text.literal("Optional, can be set later"))
                .build());
        addSub.add(eb.startIntField(Text.literal("X"), 20)
                .setMin(0)
                .setMax(6000)
                .setSaveConsumer(newX::set)
                .build());
        addSub.add(eb.startIntField(Text.literal("Y"), 40)
                .setMin(0)
                .setMax(6000)
                .setSaveConsumer(newY::set)
                .build());
        addSub.add(eb.startIntField(Text.literal("Width"), 110)
                .setMin(50)
                .setMax(240)
                .setSaveConsumer(newW::set)
                .build());
        addSub.add(eb.startIntField(Text.literal("Height"), 20)
                .setMin(18)
                .setMax(40)
                .setSaveConsumer(newH::set)
                .build());

        category.addEntry(addSub.build());

        builder.setSavingRunnable(() -> {
            MacroPanelDataHandler.PanelConfig next = new MacroPanelDataHandler.PanelConfig();
            next.title = panelTitle.get() == null ? "Macro Panel" : panelTitle.get().trim();
            next.enabled = panelEnabled[0];
            next.buttons = new ArrayList<>();

            for (ButtonState state : states) {
                if (state.delete()[0]) {
                    continue;
                }
                MacroPanelDataHandler.PanelButton b = MacroPanelDataHandler.createDefaultButton();
                b.id = state.id();
                b.label = state.label().get();
                b.macroId = state.macroId().get();
                b.x = state.x().get();
                b.y = state.y().get();
                b.width = state.width().get();
                b.height = state.height().get();
                next.buttons.add(b);
            }

            if (createNew[0]) {
                MacroPanelDataHandler.PanelButton b = MacroPanelDataHandler.createDefaultButton();
                b.label = newLabel.get();
                b.macroId = newMacroId.get();
                b.x = newX.get();
                b.y = newY.get();
                b.width = newW.get();
                b.height = newH.get();
                next.buttons.add(b);
            }

            MacroPanelDataHandler.setConfig(next);
        });

        return builder.build();
    }
}

