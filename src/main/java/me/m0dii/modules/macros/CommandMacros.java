package me.m0dii.modules.macros;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandMacros {

    private CommandMacros() {
    }

    private static final Map<String, Integer> macroKeycodes = new HashMap<>();
    private static final Map<String, Integer> macroModifierKeycodes = new HashMap<>();
    private static final Map<Integer, Boolean> keyPrevPressed = new HashMap<>();
    private static final List<PendingRun> pendingRuns = new ArrayList<>();

    private record PendingRun(@NotNull String name, @NotNull List<String> commands, int ticksRemaining) {
        PendingRun tick() {
            return new PendingRun(name, commands, Math.max(0, ticksRemaining - 1));
        }

        boolean due() {
            return ticksRemaining <= 0;
        }
    }

    public static void register() {
        MacroDataHandler.loadMacros();

        refreshKeybindings();

        ClientTickEvents.END_CLIENT_TICK.register(getEndTick());
    }

    private static ClientTickEvents.@NotNull EndTick getEndTick() {
        return client -> {
            if (client.player == null) {
                return;
            }

            // Process delayed runs first (non-blocking countdown per tick)
            if (!pendingRuns.isEmpty()) {
                for (int i = pendingRuns.size() - 1; i >= 0; i--) {
                    PendingRun pr = pendingRuns.get(i).tick();
                    if (pr.due()) {
                        executeMacro(pr.name(), pr.commands());
                        pendingRuns.remove(i);
                    } else {
                        pendingRuns.set(i, pr);
                    }
                }
            }

            if (client.currentScreen != null) {
                return;
            }

            final long handle = client.getWindow().getHandle();

            Map<Integer, Boolean> currentStates = new HashMap<>();

            for (Map.Entry<String, Integer> entry : macroKeycodes.entrySet()) {
                String macroId = entry.getKey();
                int keyCode = entry.getValue();
                if (keyCode < 0) {
                    continue;
                }

                boolean isPressed = InputUtil.isKeyPressed(handle, keyCode);
                boolean modifierPressed = true;
                int modCode = macroModifierKeycodes.getOrDefault(macroId, -1);
                if (modCode >= 0) {
                    modifierPressed = InputUtil.isKeyPressed(handle, modCode);
                }

                boolean wasPressed = keyPrevPressed.getOrDefault(keyCode, false);
                currentStates.put(keyCode, isPressed);

                if (isPressed && modifierPressed && !wasPressed) {
                    MacroDataHandler.MacroEntry macro = MacroDataHandler.getMacro(macroId);
                    if (macro != null) {
                        int delay = macro.delayTicks;
                        if (delay > 0) {
                            pendingRuns.add(new PendingRun(macro.name, new ArrayList<>(macro.commands), delay));
                        } else {
                            executeMacro(macro.name, macro.commands);
                        }
                    }
                }
            }

            keyPrevPressed.clear();
            keyPrevPressed.putAll(currentStates);
        };
    }

    public static void refreshKeybindings() {
        macroKeycodes.clear();
        macroModifierKeycodes.clear();
        for (Map.Entry<String, MacroDataHandler.MacroEntry> entry : MacroDataHandler.getAllMacros().entrySet()) {
            String macroId = entry.getKey();
            MacroDataHandler.MacroEntry macro = entry.getValue();
            macroKeycodes.put(macroId, macro.keyCode);
            macroModifierKeycodes.put(macroId, toKeyCodeFromTranslation(macro.modifierKey));
        }
        keyPrevPressed.keySet().removeIf(key -> !macroKeycodes.containsValue(key));
    }

    private static int toKeyCodeFromTranslation(String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return -1;
        }
        var key = InputUtil.fromTranslationKey(translationKey.toLowerCase(Locale.ROOT));
        return key != null ? key.getCode() : -1;
    }

    public static boolean addMacro(@NotNull String id,
                                   @NotNull String name,
                                   @NotNull List<String> commands,
                                   int keyCode,
                                   @Nullable String modifierKey,
                                   int delayTicks,
                                   boolean showInOverlay) {
        if (MacroDataHandler.hasMacro(id)) {
            return false;
        }

        MacroDataHandler.addMacro(id, name, commands, keyCode, modifierKey, delayTicks, showInOverlay);
        refreshKeybindings();
        return true;
    }

    public static boolean removeMacro(String id) {
        if (!MacroDataHandler.hasMacro(id)) {
            return false;
        }

        MacroDataHandler.removeMacro(id);
        refreshKeybindings();
        return true;
    }

    public static boolean updateMacro(String id, String name, List<String> commands, int keyCode, String modifierKey) {
        if (!MacroDataHandler.hasMacro(id)) {
            return false;
        }

        MacroDataHandler.updateMacro(id, name, commands, keyCode, modifierKey, 0, false);
        refreshKeybindings();
        return true;
    }

    public static List<String> getPendingDisplayLines() {
        if (pendingRuns.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>(pendingRuns.size());
        for (PendingRun pr : pendingRuns) {
            lines.add(pr.name + " (" + pr.ticksRemaining + "t)");
        }
        return lines;
    }

    private static void executeMacro(@NotNull String macroName, @Nullable List<String> commands) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (commands == null || commands.isEmpty()) {
            client.player.sendMessage(
                    Text.literal("§cMacro '" + macroName + "' has no commands configured!"),
                    false
            );
            return;
        }

        for (String raw : commands) {
            if (raw == null) {
                continue;
            }
            // Expand placeholders first
            String expanded = MacroPlaceholders.expand(client, raw).trim();
            if (expanded.isEmpty()) {
                continue;
            }

            // Conditional syntax support (safer separators):
            // if:<cond>::<commandIfTrue>:else:<commandIfFalse?>
            // Example: if:{player.gamemode}==creative::cmd:/give @s diamond_sword:else:cmd:/say not creative
            if (expanded.startsWith("if:")) {
                try {
                    String rest = expanded.substring(3).trim();
                    // Find the first '::' which separates condition and command(s)
                    int sepIdx = rest.indexOf("::");
                    if (sepIdx > 0) {
                        String cond = rest.substring(0, sepIdx).trim();
                        String cmds = rest.substring(sepIdx + 2);
                        String trueCmd = cmds;
                        String falseCmd = null;
                        // Use ':else:' inside the commands to split true/false branches (optional)
                        int elseIdx = cmds.indexOf(":else:");
                        if (elseIdx >= 0) {
                            trueCmd = cmds.substring(0, elseIdx);
                            falseCmd = cmds.substring(elseIdx + 6);
                        }

                        // Only support equality '==' and inequality '!=' for now
                        boolean neg = false;
                        String left = null;
                        String right = null;
                        if (cond.contains("==")) {
                            String[] parts = cond.split("==", 2);
                            left = parts[0].trim();
                            right = parts[1].trim();
                        } else if (cond.contains("!=")) {
                            String[] parts = cond.split("!=", 2);
                            left = parts[0].trim();
                            right = parts[1].trim();
                            neg = true;
                        }

                        if (left != null && right != null) {
                            // Expand placeholders inside left and right if present
                            String leftVal = MacroPlaceholders.expand(client, left);
                            String rightVal = MacroPlaceholders.expand(client, right);
                            boolean matches = leftVal.equalsIgnoreCase(rightVal);
                            if (neg) {
                                matches = !matches;
                            }

                            String toRun = matches ? trueCmd : falseCmd;
                            if (toRun != null && !toRun.isBlank()) {
                                List<String> single = List.of(toRun.trim());
                                executeMacro(macroName, single);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                continue;
            }

            try {
                // Typed prefixes: 'msg:'|'say:' for chat, 'cmd:' or leading '/' for commands
                if (expanded.startsWith("msg:") || expanded.startsWith("say:")) {
                    String msg = expanded.substring(expanded.indexOf(':') + 1).trim();
                    if (!msg.isEmpty()) {
                        client.player.networkHandler.sendChatMessage(msg);
                    }
                    continue;
                }

                if (expanded.startsWith("bar:")) {
                    String msg = expanded.substring(4).trim();
                    if (!msg.isEmpty()) {
                        client.player.sendMessage(Text.literal(msg), true);
                    }
                    continue;
                }

                if (expanded.startsWith("copy:")) {
                    String msg = expanded.substring(5).trim();
                    if (!msg.isEmpty()) {
                        client.keyboard.setClipboard(msg);
                        client.player.sendMessage(Text.literal("§7Copied to clipboard"), true);
                    }
                    continue;
                }

                if (expanded.startsWith("cmd:")) {
                    expanded = expanded.substring(4).trim();
                }

                if (expanded.startsWith("/")) {
                    client.player.networkHandler.sendChatCommand(expanded.substring(1));
                } else {
                    // Plain chat message
                    client.player.networkHandler.sendChatMessage(expanded);
                }
            } catch (Exception e) {
                client.player.sendMessage(
                        Text.literal("§cMacro '" + macroName + "' failed: " + e.getMessage()),
                        false
                );
            }
        }
    }
}
