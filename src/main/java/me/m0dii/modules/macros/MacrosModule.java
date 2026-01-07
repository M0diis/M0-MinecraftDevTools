package me.m0dii.modules.macros;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.m0dii.modules.Module;
import me.m0dii.modules.macros.gui.MacroConfigScreen;
import me.m0dii.modules.macros.gui.MacroKeybindOverlayModule;
import me.m0dii.modules.macros.gui.PendingMacrosOverlayModule;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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

        registerPressedKeybind(
                "key.m0-dev-tools.open_macro_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M, // Default: M key
                client -> client.setScreen(MacroConfigScreen.create(client.currentScreen))
        );

        MacroKeybindOverlayModule.INSTANCE.register();
        PendingMacrosOverlayModule.INSTANCE.register();
    }

    private static void registerMacroCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("macro")
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                        .then(ClientCommandManager.argument("keycode", IntegerArgumentType.integer())
                                                .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                                                        .executes(MacrosModule::addMacro))))))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                .executes(MacrosModule::removeMacro)))
                .then(ClientCommandManager.literal("update")
                        .then(ClientCommandManager.argument("id", StringArgumentType.word())
                                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                        .then(ClientCommandManager.argument("keycode", IntegerArgumentType.integer())
                                                .then(ClientCommandManager.argument("commands", StringArgumentType.greedyString())
                                                        .executes(MacrosModule::updateMacro))))))
                .then(ClientCommandManager.literal("list")
                        .executes(MacrosModule::listMacros))
                .then(ClientCommandManager.literal("help")
                        .executes(MacrosModule::showHelp))
        );
    }

    private static int addMacro(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        int keyCode = IntegerArgumentType.getInteger(context, "keycode");
        String modifierKey = ""; // Future use for modifier keys like Shift, Ctrl, etc.
        List<String> commands = List.of(StringArgumentType.getString(context, "commands").split(";"));

        if (CommandMacros.addMacro(id, name, commands, keyCode, modifierKey, 0, false)) {
            context.getSource().sendFeedback(Text.literal("§aAdded macro '" + name + "' with ID '" + id + "' (Key: " + getKeyName(keyCode) + ")"));
        } else {
            context.getSource().sendError(Text.literal("§cMacro with ID '" + id + "' already exists!"));
        }

        return 1;
    }

    private static int removeMacro(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");

        MacroDataHandler.MacroEntry macro = MacroDataHandler.getMacro(id);
        if (macro == null) {
            context.getSource().sendError(Text.literal("§cMacro with ID '" + id + "' doesn't exist!"));
            return 0;
        }

        if (CommandMacros.removeMacro(id)) {
            context.getSource().sendFeedback(Text.literal("§aRemoved macro '" + macro.name + "' (ID: " + id + ")"));
        } else {
            context.getSource().sendError(Text.literal("§cFailed to remove macro '" + id + "'"));
        }

        return 1;
    }

    private static int updateMacro(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");
        String name = StringArgumentType.getString(context, "name");
        int keyCode = IntegerArgumentType.getInteger(context, "keycode");
        String modifierKey = "";
        List<String> commands = StringArgumentType.getString(context, "command").isEmpty() ? new ArrayList<>() : List.of(StringArgumentType.getString(context, "command").split(";"));

        if (CommandMacros.updateMacro(id, name, commands, keyCode, modifierKey)) {
            context.getSource().sendFeedback(Text.literal("§aUpdated macro '" + name + "' (ID: " + id + ")"));
        } else {
            context.getSource().sendError(Text.literal("§cMacro with ID '" + id + "' doesn't exist!"));
        }

        return 1;
    }

    private static int listMacros(CommandContext<FabricClientCommandSource> context) {
        Map<String, MacroDataHandler.MacroEntry> macros = MacroDataHandler.getAllMacros();

        if (macros.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("§eNo macros configured."));
            return 1;
        }

        context.getSource().sendFeedback(Text.literal("§aCconfigured Macros:"));
        for (Map.Entry<String, MacroDataHandler.MacroEntry> entry : macros.entrySet()) {
            String id = entry.getKey();
            MacroDataHandler.MacroEntry macro = entry.getValue();
            context.getSource().sendFeedback(Text.literal(
                    "§f- §b" + macro.name + " §f(ID: §e" + id + "§f, Key: §a" + getKeyName(macro.keyCode) + "§f): §7" + macro.commands.stream()
                            .reduce((a, b) -> a + "; " + b).orElse("")
            ));
        }

        return 1;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6=== Macro Commands Help ==="));
        context.getSource().sendFeedback(Text.literal("§a/macro add <id> <name> <keycode> <command>§f - Add a new macro"));
        context.getSource().sendFeedback(Text.literal("§a/macro remove <id>§f - Remove a macro"));
        context.getSource().sendFeedback(Text.literal("§a/macro update <id> <name> <keycode> <command>§f - Update existing macro"));
        context.getSource().sendFeedback(Text.literal("§a/macro list§f - List all configured macros"));
        context.getSource().sendFeedback(Text.literal("§a/macro help§f - Show this help"));
        context.getSource().sendFeedback(Text.literal(""));
        context.getSource().sendFeedback(Text.literal("§eExamples:"));
        context.getSource().sendFeedback(Text.literal("§7/macro add creative \"Creative Mode\" " + GLFW.GLFW_KEY_F1 + " gamemode creative"));
        context.getSource().sendFeedback(Text.literal("§7/macro add tp_up \"Teleport Up\" " + GLFW.GLFW_KEY_F2 + " tp ~ ~10 ~"));
        context.getSource().sendFeedback(Text.literal(""));

        return 1;
    }

    private static String getKeyName(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_1 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return "Numpad " + (keyCode - GLFW.GLFW_KEY_KP_1 + 1);
        }
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + keyCode - GLFW.GLFW_KEY_A));
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf(keyCode - GLFW.GLFW_KEY_0);
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt";
            default -> "Key " + keyCode;
        };
    }
}