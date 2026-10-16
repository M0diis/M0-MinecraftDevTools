package me.m0dii.modules.macros;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.m0dii.modules.Module;
import me.m0dii.modules.macros.gui.MacroConfigScreen;
import me.m0dii.modules.macros.gui.MacroKeybindHudModule;
import me.m0dii.modules.macros.gui.MacroWorkbenchScreen;
import me.m0dii.modules.macros.hud.MacroHudDataHandler;
import me.m0dii.modules.macros.hud.MacroHudModule;
import me.m0dii.modules.macros.hud.PendingMacrosHudModule;
import me.m0dii.utils.KeybindCatalog;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MacrosModule extends Module {

    public static final MacrosModule INSTANCE = new MacrosModule();

    private MacrosModule() {
        super("macro_commands", "Macro Commands", true);
    }

    @Override
    public void register() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess)
                        -> registerMacroCommands(dispatcher));

        CommandMacros.register();
        ParsePlaceholderClientCommand.register();
        TargetContextPlaceholderProvider.register();
        MacroHudDataHandler.load();

        registerPressedKeybind(
                KeybindCatalog.OPEN_MACRO_GUI.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.OPEN_MACRO_GUI.defaultKey(),
                client -> client.gui.setScreen(MacroConfigScreen.create(client.gui.screen()))
        );

        registerPressedKeybind(
                KeybindCatalog.OPEN_MACRO_HUD_EDITOR.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.OPEN_MACRO_HUD_EDITOR.defaultKey(),
                client -> client.gui.setScreen(MacroWorkbenchScreen.create(client.gui.screen(), MacroWorkbenchScreen.Tab.CANVAS))
        );

        registerPressedKeybind(
                KeybindCatalog.OPEN_MACRO_KEYBOARD_LAYOUT.translationKey(),
                InputConstants.Type.KEYBOARD,
                KeybindCatalog.OPEN_MACRO_KEYBOARD_LAYOUT.defaultKey(),
                client -> client.gui.setScreen(MacroWorkbenchScreen.create(client.gui.screen(), MacroWorkbenchScreen.Tab.KEYBOARD))
        );

        MacroKeybindHudModule.INSTANCE.register();
        PendingMacrosHudModule.INSTANCE.register();
        MacroHudModule.INSTANCE.register();
    }

    private static void registerMacroCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("macro")
                .then(ClientCommands.literal("add")
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .then(ClientCommands.argument("name", StringArgumentType.string())
                                        .then(ClientCommands.argument("keycode", IntegerArgumentType.integer())
                                                .then(ClientCommands.argument("command", StringArgumentType.greedyString())
                                                        .executes(MacrosModule::addMacro))))))
                .then(ClientCommands.literal("remove")
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .executes(MacrosModule::removeMacro)))
                .then(ClientCommands.literal("update")
                        .then(ClientCommands.argument("id", StringArgumentType.word())
                                .then(ClientCommands.argument("name", StringArgumentType.string())
                                        .then(ClientCommands.argument("keycode", IntegerArgumentType.integer())
                                                .then(ClientCommands.argument("commands", StringArgumentType.greedyString())
                                                        .executes(MacrosModule::updateMacro))))))
                .then(ClientCommands.literal("list")
                        .executes(MacrosModule::listMacros))
                .then(ClientCommands.literal("help")
                        .executes(MacrosModule::showHelp))
        );
    }

    private static int addMacro(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        int keyCode = IntegerArgumentType.getInteger(context, "keycode");
        String modifierKey = ""; // Future use for modifier keys like Shift, Ctrl, etc.
        List<String> commands = List.of(StringArgumentType.getString(context, "command").split(";"));

        if (CommandMacros.addMacro(id, name, commands, keyCode, modifierKey, 0, false)) {
            context.getSource().sendFeedback(Component.literal("§aAdded macro '" + name + "' with ID '" + id + "' (Key: " + getKeyName(keyCode) + ")"));
        } else {
            context.getSource().sendError(Component.literal("§cMacro with ID '" + id + "' already exists!"));
        }

        return 1;
    }

    private static int removeMacro(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");

        MacroDataHandler.MacroEntry macro = MacroDataHandler.getMacro(id);
        if (macro == null) {
            context.getSource().sendError(Component.literal("§cMacro with ID '" + id + "' doesn't exist!"));
            return 0;
        }

        if (CommandMacros.removeMacro(id)) {
            context.getSource().sendFeedback(Component.literal("§aRemoved macro '" + macro.name + "' (ID: " + id + ")"));
        } else {
            context.getSource().sendError(Component.literal("§cFailed to remove macro '" + id + "'"));
        }

        return 1;
    }

    private static int updateMacro(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        int keyCode = IntegerArgumentType.getInteger(context, "keycode");
        String modifierKey = "";
        List<String> commands = StringArgumentType.getString(context, "commands").isEmpty() ? new ArrayList<>() : List.of(StringArgumentType.getString(context, "commands").split(";"));

        if (CommandMacros.updateMacro(id, name, commands, keyCode, modifierKey)) {
            context.getSource().sendFeedback(Component.literal("§aUpdated macro '" + name + "' (ID: " + id + ")"));
        } else {
            context.getSource().sendError(Component.literal("§cMacro with ID '" + id + "' doesn't exist!"));
        }

        return 1;
    }

    private static int listMacros(CommandContext<FabricClientCommandSource> context) {
        Map<String, MacroDataHandler.MacroEntry> macros = MacroDataHandler.getAllMacros();

        if (macros.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("§eNo macros configured."));
            return 1;
        }

        context.getSource().sendFeedback(Component.literal("§aConfigured Macros:"));
        for (Map.Entry<String, MacroDataHandler.MacroEntry> entry : macros.entrySet()) {
            String id = entry.getKey();
            MacroDataHandler.MacroEntry macro = entry.getValue();
            context.getSource().sendFeedback(Component.literal(
                    "§f- §b" + macro.name + " §f(ID: §e" + id + "§f, Key: §a" + getKeyName(macro.keyCode) + "§f): §7" + macro.commands.stream()
                            .reduce((a, b) -> a + "; " + b).orElse("")
            ));
        }

        return 1;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("§6=== Macro Commands Help ==="));
        context.getSource().sendFeedback(Component.literal("§a/macro add <id> <name> <keycode> <command>§f - Add a new macro"));
        context.getSource().sendFeedback(Component.literal("§a/macro remove <id>§f - Remove a macro"));
        context.getSource().sendFeedback(Component.literal("§a/macro update <id> <name> <keycode> <command>§f - Update existing macro"));
        context.getSource().sendFeedback(Component.literal("§a/macro list§f - List all configured macros"));
        context.getSource().sendFeedback(Component.literal("§a/macro help§f - Show this help"));
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("§eExamples:"));
        context.getSource().sendFeedback(Component.literal("§7/macro add creative \"Creative Mode\" " + InputConstants.KEY_F1 + " gamemode creative"));
        context.getSource().sendFeedback(Component.literal("§7/macro add tp_up \"Teleport Up\" " + InputConstants.KEY_F2 + " tp ~ ~10 ~"));
        context.getSource().sendFeedback(Component.literal(""));

        return 1;
    }

    private static String getKeyName(int keyCode) {
        if (keyCode >= InputConstants.KEY_F1 && keyCode <= InputConstants.KEY_F12) {
            return "F" + (keyCode - InputConstants.KEY_F1 + 1);
        }
        if (keyCode >= InputConstants.KEY_NUMPAD1 && keyCode <= InputConstants.KEY_NUMPAD9) {
            return "Numpad " + (keyCode - InputConstants.KEY_NUMPAD1 + 1);
        }
        if (keyCode >= InputConstants.KEY_A && keyCode <= InputConstants.KEY_Z) {
            return String.valueOf((char) ('A' + keyCode - InputConstants.KEY_A));
        }
        // Scancode order is 1..9 then 0.
        if (keyCode == InputConstants.KEY_0) {
            return "0";
        }
        if (keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9) {
            return String.valueOf(keyCode - InputConstants.KEY_1 + 1);
        }

        return switch (keyCode) {
            case InputConstants.KEY_SPACE -> "Space";
            case InputConstants.KEY_RETURN -> "Enter";
            case InputConstants.KEY_TAB -> "Tab";
            case InputConstants.KEY_LSHIFT -> "Left Shift";
            case InputConstants.KEY_RSHIFT -> "Right Shift";
            case InputConstants.KEY_LCONTROL -> "Left Ctrl";
            case InputConstants.KEY_RCONTROL -> "Right Ctrl";
            case InputConstants.KEY_LALT -> "Left Alt";
            case InputConstants.KEY_RALT -> "Right Alt";
            default -> "Key " + keyCode;
        };
    }
}
