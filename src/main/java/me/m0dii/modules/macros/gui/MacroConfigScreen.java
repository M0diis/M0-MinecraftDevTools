package me.m0dii.modules.macros.gui;

import me.m0dii.modules.macros.CommandMacros;
import me.m0dii.modules.macros.MacroDataHandler;
import me.m0dii.utils.ModConfig;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class MacroConfigScreen {
    private MacroConfigScreen() {
    }

    record MacroState(
            String id,
            AtomicReference<String> name,
            List<String> commands,
            AtomicInteger keyCode,
            AtomicReference<String> modifierKey,
            AtomicInteger delayTicks,
            boolean[] showInOverlay,
            boolean[] deleteFlag
    ) {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setShouldListSmoothScroll(true)
                .setShouldTabsSmoothScroll(true)
                .solidBackground()
                .setTitle(Text.literal("Macro Manager"));

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Text.literal("Macros"));
        boolean[] overlayVisible = new boolean[]{ModConfig.showMacroKeybindOverlay};

        SubCategoryBuilder overlaySettings = eb.startSubCategory(Text.literal("Overlay Settings"))
                .setExpanded(false);
        overlaySettings.add(eb.startBooleanToggle(Text.literal("Show macro keybind overlay"), overlayVisible[0])
                .setTooltip(Text.literal("Shows or hides the macro keybind list on the HUD"))
                .setSaveConsumer(val -> overlayVisible[0] = val)
                .build());
        category.addEntry(overlaySettings.build());

        SubCategoryBuilder subInstructors = eb.startSubCategory(Text.literal("Instructions"))
                .setExpanded(false);

        subInstructors.add(eb.startTextDescription(
                Text.literal("""
                        Commands prefixes:
                         • cmd:... or /... => server command
                         • msg:... or say:... => chat message
                         • bar:... => action bar message
                         • copy:... => copy text to clipboard
                        
                        Placeholders (use {token}):
                         • {player.___} (name, uuid, hp, max_hp, food, saturation, xp, level, yaw, pitch, facing, gamemode)
                         • {pos.___} (x, y, z, biome, dim, light, block, facing)
                         • {sel.self/@s}, {sel.nearest/@p}, {sel.random/@r}, {sel.all/@a}, {sel.entities/@e}
                         • {players.count}, {players.count.other}
                         • {players.list}, {players.list.other}, {players.list.nl}, {players.list.other.nl}
                         • {players.nearby.3}, {players.nearby.3.nl}, {players.nearby.3.r128}, {players.nearby.3.r128.nl}
                         • {players.nearby.3.with_distance}, {players.nearby.3.with_distance.nl}, {players.nearby.3.unique}
                         • sort suffixes: .sort=name or .sort=distance (default distance)
                         • {entities.nearby.3}, {entities.nearby.3.nl}, {entities.nearby.3.r64}, {entities.nearby.3.r64.nl}
                         • {entities.nearby.3.with_distance}, {entities.nearby.3.with_distance.nl}, {entities.nearby.3.unique}
                         • {look.block.xyz}, {look.entity.name}, {dim}, rand.int(a,b)
                         • {hand.___} (item, id, count, damage, max_damage, durability)
                        
                        Conditional syntax support:
                         • if:<cond>::<command_if_true>:else:<command_if_false?> (or omit :else: part)
                         • Example: if:{player.gamemode}==creative::cmd:/gamemode spectator:else:cmd:/gamemode creative
                        """
                )
        ).build());

        category.addEntry(subInstructors.build());

        Map<String, MacroDataHandler.MacroEntry> snapshot = MacroDataHandler.getAllMacros();

        List<MacroState> states = new ArrayList<>();

        for (Map.Entry<String, MacroDataHandler.MacroEntry> e : snapshot.entrySet()) {
            String id = e.getKey();
            MacroDataHandler.MacroEntry macroEntry = e.getValue();

            AtomicReference<String> nameRef = new AtomicReference<>(macroEntry.name != null ? macroEntry.name : "");
            List<String> cmdsRef = new ArrayList<>(macroEntry.commands != null ? macroEntry.commands : List.of());
            AtomicInteger keyRef = new AtomicInteger(macroEntry.keyCode);
            AtomicInteger delayTicks = new AtomicInteger(macroEntry.delayTicks);
            AtomicReference<String> modifierKey = new AtomicReference<>(macroEntry.modifierKey != null ? macroEntry.modifierKey : "");
            boolean[] showInOverlay = new boolean[]{macroEntry.showInOverlay};
            boolean[] del = new boolean[]{false};

            // Build title with key and optional modifier
            String baseKey = (macroEntry.keyCode == -1)
                    ? "None"
                    : InputUtil.Type.KEYSYM.createFromCode(macroEntry.keyCode).getTranslationKey().toUpperCase();
            String modPart = null;
            if (macroEntry.modifierKey != null && !macroEntry.modifierKey.isEmpty() && macroEntry.keyCode != -1) {
                var modKey = InputUtil.fromTranslationKey(macroEntry.modifierKey.toLowerCase());
                if (modKey != null) {
                    modPart = modKey.getTranslationKey().toUpperCase();
                }
            }
            String combined = (modPart == null) ? baseKey : (modPart + " + " + baseKey);

            Text title = Text.literal((macroEntry.name == null || macroEntry.name.isEmpty()) ? id : macroEntry.name)
                    .append(Text.literal(" (" + combined + ")")
                            .formatted(macroEntry.keyCode == -1 ? Formatting.RED : Formatting.GRAY));

            SubCategoryBuilder sub = eb.startSubCategory(title)
                    .setExpanded(false);

            sub.add(eb.startStrField(Text.literal("Name"), nameRef.get())
                    .setTooltip(Text.literal("Display name of the macro"))
                    .setSaveConsumer(nameRef::set)
                    .build());

            sub.add(eb.startKeyCodeField(Text.literal("Key"), InputUtil.Type.KEYSYM.createFromCode(Math.max(-1, keyRef.get())))
                    .setTooltip(Text.literal("Keyboard key to trigger this macro"))
                    .setDefaultValue(InputUtil.UNKNOWN_KEY)
                    .setKeySaveConsumer(k -> keyRef.set(k.getCode()))
                    .build());

            sub.add(eb.startModifierKeyCodeField(Text.literal("Modifier"), modifierKey.get() == null || modifierKey.get().isEmpty()
                            ? ModifierKeyCode.of(InputUtil.UNKNOWN_KEY, Modifier.none())
                            : ModifierKeyCode.of(InputUtil.fromTranslationKey(modifierKey.get().toLowerCase()), Modifier.none()))
                    .setTooltip(Text.literal("Optional modifier key (CTRL, ALT, SHIFT)"))
                    .setDefaultValue(ModifierKeyCode.of(InputUtil.UNKNOWN_KEY, Modifier.none()))
                    .setModifierSaveConsumer(val -> {
                        if (val == null || val.getKeyCode() == null || val.getKeyCode().getCode() == -1) {
                            modifierKey.set("");
                        } else {
                            modifierKey.set(val.getKeyCode().getTranslationKey().toLowerCase());
                        }
                    })
                    .build());

            sub.add(eb.startStrList(Text.literal("Commands"), new ArrayList<>(cmdsRef))
                    .setTooltip(Text.literal("List of steps. One per line. Supports prefixes and {placeholders}"))
                    .setSaveConsumer(list -> {
                        cmdsRef.clear();
                        cmdsRef.addAll(list);
                    })
                    .build());

            sub.add(eb.startIntField(Text.literal("Delay Ticks"), delayTicks.get())
                    .setTooltip(Text.literal("Delay between commands in ticks (20 ticks = 1 second)"))
                    .setMin(0)
                    .setMax(1000)
                    .setDefaultValue(0)
                    .setSaveConsumer(delay -> {
                        if (delay != null) {
                            delayTicks.set(delay);
                        }
                    })
                    .build());

            sub.add(eb.startBooleanToggle(Text.literal("Show in overlay"), showInOverlay[0])
                    .setTooltip(Text.literal("Show this macro in the HUD overlay when it's running"))
                    .setSaveConsumer(val -> showInOverlay[0] = val)
                    .build());

            sub.add(eb.startBooleanToggle(Text.literal("Delete"), false)
                    .setTooltip(Text.literal("Mark this macro to be deleted when saving"))
                    .setSaveConsumer(val -> del[0] = val)
                    .build());

            category.addEntry(sub.build());
            states.add(new MacroState(id, nameRef, cmdsRef, keyRef, modifierKey, delayTicks, showInOverlay, del));
        }

        // New macro inputs
        AtomicReference<String> newName = new AtomicReference<>("");
        List<String> newCmds = new ArrayList<>();
        AtomicInteger newKey = new AtomicInteger(-1);
        AtomicReference<String> newModifierKey = new AtomicReference<>("");
        AtomicInteger newDelayTicks = new AtomicInteger(0);
        boolean[] newShowInOverlay = new boolean[]{false};

        SubCategoryBuilder addSub = eb.startSubCategory(Text.literal("Add Macro"))
                .setExpanded(false);
        addSub.add(eb.startStrField(Text.literal("Name"), "")
                .setTooltip(Text.literal("Name for the new macro"))
                .setSaveConsumer(newName::set)
                .build());
        addSub.add(eb.startKeyCodeField(Text.literal("Key"), InputUtil.UNKNOWN_KEY)
                .setTooltip(Text.literal("Key for the new macro"))
                .setDefaultValue(InputUtil.UNKNOWN_KEY)
                .setKeySaveConsumer(k -> newKey.set(k.getCode()))
                .build());
        addSub.add(eb.startModifierKeyCodeField(Text.literal("Modifier"), newModifierKey.get() == null || newModifierKey.get().isEmpty()
                        ? ModifierKeyCode.of(InputUtil.fromTranslationKey("key.keyboard.unknown"), Modifier.none())
                        : ModifierKeyCode.of(InputUtil.fromTranslationKey(newModifierKey.get().toLowerCase()), Modifier.none()))
                .setTooltip(Text.literal("Optional modifier key (CTRL, ALT, SHIFT)"))
                .setDefaultValue(ModifierKeyCode.of(InputUtil.UNKNOWN_KEY, Modifier.none()))
                .setModifierSaveConsumer(val -> {
                    if (val == null || val.getKeyCode() == null || val.getKeyCode().getCode() == -1) {
                        newModifierKey.set("");
                    } else {
                        newModifierKey.set(val.getKeyCode().getTranslationKey().toLowerCase());
                    }
                })
                .build());

        addSub.add(eb.startStrList(Text.literal("Commands"), new ArrayList<>(List.of("/")))
                .setTooltip(Text.literal("List of steps. One per line. Supports prefixes and {placeholders}"))
                .setSaveConsumer(list -> {
                    newCmds.clear();
                    newCmds.addAll(list);
                })
                .setDefaultValue(List.of("/"))
                .build());
        addSub.add(eb.startIntField(Text.literal("Delay Ticks"), 0)
                .setTooltip(Text.literal("Delay between commands in ticks (20 ticks = 1 second)"))
                .setMin(0)
                .setMax(1000)
                .setDefaultValue(0)
                .setSaveConsumer(newDelayTicks::set)
                .build());
        addSub.add(eb.startBooleanToggle(Text.literal("Show in overlay"), false)
                .setTooltip(Text.literal("Show this macro in the HUD overlay when it's running"))
                .setSaveConsumer(val -> newShowInOverlay[0] = val)
                .build());

        category.addEntry(addSub.build());

        builder.setSavingRunnable(() -> {
            ModConfig.updateAndSave(() -> ModConfig.showMacroKeybindOverlay = overlayVisible[0]);

            // Apply deletions and updates
            for (MacroState state : states) {
                if (state.deleteFlag()[0]) {
                    MacroDataHandler.removeMacro(state.id());
                    continue;
                }
                String name = state.name().get().trim();
                List<String> cmds = sanitize(state.commands());
                int key = state.keyCode().get();
                String modifierKey = state.modifierKey().get();
                int delayTicks = state.delayTicks().get();
                boolean showInOverlay = state.showInOverlay()[0];
                MacroDataHandler.updateMacro(state.id(), name, cmds, key, modifierKey, delayTicks, showInOverlay);
            }
            // Add new macro if valid
            String nn = newName.get().trim();
            List<String> nc = sanitize(newCmds);
            int nk = newKey.get();
            String modifierKey = newModifierKey.get();
            int ndelay = newDelayTicks.get();
            boolean newOverlayShow = newShowInOverlay[0];
            if (!nn.isEmpty() && !nc.isEmpty() && nk != -1) {
                String newId = UUID.randomUUID().toString().substring(0, 8);
                MacroDataHandler.addMacro(newId, nn, nc, nk, modifierKey, ndelay, newOverlayShow);
            }
            // Refresh the runtime key polling map
            CommandMacros.refreshKeybindings();
        });

        return builder.build();
    }

    private static List<String> sanitize(@Nullable List<String> src) {
        if (src == null) {
            return new ArrayList<>();
        }
        return src.stream().filter(Objects::nonNull).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }
}
