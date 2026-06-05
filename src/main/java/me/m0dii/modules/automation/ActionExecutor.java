package me.m0dii.modules.automation;

import me.m0dii.modules.macros.MacroPlaceholders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionExecutor {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^}]+)}");

    public interface Bridge {
        boolean runMacro(@NotNull String macroId);

        ActionResult runScript(@NotNull String scriptFile, @NotNull Map<String, Object> context);

        boolean sendClientCommand(@NotNull String command);

        boolean toggleModule(@NotNull String moduleId, @Nullable Boolean enabledState);

        @Nullable MinecraftClient client();

        @Nullable ClientPlayerEntity player();

        @Nullable ClientWorld world();
    }

    public record ActionResult(boolean success, String message) {
        public static ActionResult ok(String message) {
            return new ActionResult(true, message);
        }

        public static ActionResult error(String message) {
            return new ActionResult(false, message);
        }
    }

    private final Bridge bridge;

    public ActionExecutor(@NotNull Bridge bridge) {
        this.bridge = Objects.requireNonNull(bridge);
    }

    public ActionResult execute(@NotNull AutomationRule rule, @NotNull AutomationEvent event) {
        if (rule.actions == null || rule.actions.isEmpty()) {
            return ActionResult.error("Rule has no actions.");
        }

        List<String> failures = new ArrayList<>();
        int executed = 0;
        for (AutomationRule.Action action : rule.actions) {
            if (action == null || !action.enabled) {
                continue;
            }
            ActionResult result = executeAction(action, event);
            if (!result.success()) {
                failures.add(result.message());
            } else {
                executed++;
            }
        }

        if (executed <= 0 && failures.isEmpty()) {
            return ActionResult.error("Rule has no enabled actions.");
        }
        if (failures.isEmpty()) {
            return ActionResult.ok("Executed " + executed + " action(s).");
        }
        if (executed > 0) {
            return ActionResult.error("Executed " + executed + " action(s), " + failures.size() + " failed: " + String.join(" | ", failures));
        }
        return ActionResult.error(String.join(" | ", failures));
    }

    public ActionResult executeAction(@NotNull AutomationRule.Action action, @NotNull AutomationEvent event) {
        AutomationActionType type = action.type == null ? AutomationActionType.SEND_CLIENT_COMMAND : action.type;
        String target = expandActionText(action.target, event).trim();
        if (target.isEmpty()) {
            return ActionResult.error("Action target is empty for " + type.name());
        }
        String argument = expandActionText(action.argument, event).trim();
        Map<String, String> parameters = expandParameters(action.parameters, event);

        return switch (type) {
            case RUN_MACRO -> bridge.runMacro(target)
                    ? ActionResult.ok("Ran macro '" + target + "'.")
                    : ActionResult.error("Macro '" + target + "' not found or failed.");
            case RUN_SCRIPT -> bridge.runScript(target, buildScriptContext(event));
            case SEND_CLIENT_COMMAND -> bridge.sendClientCommand(target)
                    ? ActionResult.ok("Sent client command '" + target + "'.")
                    : ActionResult.error("Failed to send client command '" + target + "'.");
            case TOGGLE_MODULE -> {
                Boolean state = parseState(argument, parameters);
                boolean changed = bridge.toggleModule(target, state);
                String suffix = state == null ? "toggle" : (state ? "enable" : "disable");
                yield changed
                        ? ActionResult.ok("Module '" + target + "' " + suffix + " dispatched.")
                        : ActionResult.error("Module '" + target + "' was not found.");
            }
        };
    }

    protected Map<String, Object> buildScriptContext(@NotNull AutomationEvent event) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("event", event);
        context.put("client", bridge.client());
        context.put("player", bridge.player());
        context.put("world", bridge.world());
        context.put("source", bridge.player());
        return context;
    }

    private String expandActionText(@Nullable String raw, @NotNull AutomationEvent event) {
        String input = raw == null ? "" : raw;
        if (input.isEmpty()) {
            return "";
        }
        String contextual = expandAutomationPlaceholders(input, event);
        MinecraftClient client = bridge.client();
        if (client == null) {
            return contextual;
        }
        return MacroPlaceholders.expand(client, contextual);
    }

    private String expandAutomationPlaceholders(@NotNull String input, @NotNull AutomationEvent event) {
        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1).trim();
            String replacement = resolveAutomationPlaceholder(token, event);
            if (replacement == null) {
                matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private @Nullable String resolveAutomationPlaceholder(@Nullable String token, @NotNull AutomationEvent event) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = token.trim();
        if (normalized.startsWith("event.")) {
            return stringify(event.attribute(normalized.substring("event.".length())));
        }
        if (normalized.startsWith("client.")) {
            return stringify(clientValue(normalized.substring("client.".length())));
        }
        if (normalized.startsWith("player.")) {
            return stringify(playerValue(normalized.substring("player.".length())));
        }
        if (normalized.startsWith("world.")) {
            return stringify(worldValue(normalized.substring("world.".length())));
        }
        return null;
    }

    private @Nullable Object clientValue(@NotNull String key) {
        MinecraftClient client = bridge.client();
        if (client == null) {
            return null;
        }
        return switch (key) {
            case "connected" -> bridge.player() != null && bridge.world() != null;
            case "screen" -> client.currentScreen == null ? "" : client.currentScreen.getClass().getSimpleName();
            case "screenPresent" -> client.currentScreen != null;
            case "paused" -> client.isPaused();
            case "worldLoaded" -> bridge.world() != null;
            case "playerLoaded" -> bridge.player() != null;
            default -> null;
        };
    }

    private @Nullable Object playerValue(@NotNull String key) {
        ClientPlayerEntity player = bridge.player();
        ClientWorld world = bridge.world();
        if (player == null) {
            return null;
        }
        return switch (key) {
            case "name" -> player.getName().getString();
            case "x" -> player.getX();
            case "y" -> player.getY();
            case "z" -> player.getZ();
            case "health" -> player.getHealth();
            case "maxHealth" -> player.getMaxHealth();
            case "food" -> player.getHungerManager().getFoodLevel();
            case "sneaking" -> player.isSneaking();
            case "sprinting" -> player.isSprinting();
            case "dimension" -> world == null ? "" : world.getRegistryKey().getValue().toString();
            case "hotbarSlot" -> player.getInventory().getSelectedSlot();
            case "mainHandItemId" -> player.getMainHandStack().isEmpty() ? "" : net.minecraft.registry.Registries.ITEM.getId(player.getMainHandStack().getItem()).toString();
            case "mainHandItemName" -> player.getMainHandStack().isEmpty() ? "" : player.getMainHandStack().getName().getString();
            default -> null;
        };
    }

    private @Nullable Object worldValue(@NotNull String key) {
        ClientWorld world = bridge.world();
        if (world == null) {
            return null;
        }
        return switch (key) {
            case "dimension" -> world.getRegistryKey().getValue().toString();
            case "time" -> world.getTime();
            case "raining" -> world.isRaining();
            case "thundering" -> world.isThundering();
            default -> null;
        };
    }

    private Map<String, String> expandParameters(@Nullable Map<String, String> parameters, @NotNull AutomationEvent event) {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        Map<String, String> expanded = new LinkedHashMap<>();
        parameters.forEach((key, value) -> expanded.put(key, expandActionText(value, event)));
        return expanded;
    }

    private static @Nullable Boolean parseState(@Nullable String argument, @NotNull Map<String, String> parameters) {
        if (!parameters.isEmpty()) {
            String state = parameters.get("state");
            if (state != null && !state.isBlank()) {
                return parseBoolean(state);
            }
        }
        if (argument != null && !argument.isBlank()) {
            return parseBoolean(argument);
        }
        return null;
    }

    private static String stringify(@Nullable Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static @Nullable Boolean parseBoolean(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "on", "enable", "enabled", "1" -> true;
            case "false", "off", "disable", "disabled", "0" -> false;
            default -> null;
        };
    }
}
