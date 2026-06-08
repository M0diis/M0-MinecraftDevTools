package me.m0dii.modules.automation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class AutomationCommands {
    private AutomationCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, AutomationModule module) {
        var root = literal("automation")
                .then(literal("ui").executes(ctx -> openUi(ctx, module)))
                .then(literal("open").executes(ctx -> openUi(ctx, module)))
                .then(literal("reload").executes(ctx -> reload(ctx, module)))
                .then(literal("save").executes(ctx -> save(ctx, module)))
                .then(literal("list").executes(ctx -> listRules(ctx, module)))
                .then(literal("path").executes(AutomationCommands::printConfigPath))
                .then(literal("enable")
                        .then(argument("id", StringArgumentType.word())
                                .executes(ctx -> setRuleEnabled(ctx, module, true))))
                .then(literal("disable")
                        .then(argument("id", StringArgumentType.word())
                                .executes(ctx -> setRuleEnabled(ctx, module, false))))
                .then(literal("logs")
                        .executes(ctx -> printLogs(ctx, module, 10))
                        .then(argument("count", IntegerArgumentType.integer(1, 50))
                                .executes(ctx -> printLogs(ctx, module, IntegerArgumentType.getInteger(ctx, "count")))))
                .then(literal("fire")
                        .then(literal("tick").executes(ctx -> fireTick(ctx, module)))
                        .then(literal("move").executes(ctx -> fireMove(ctx, module)))
                        .then(literal("world_join").executes(ctx -> fireWorldJoin(ctx, module)))
                        .then(literal("world_leave").executes(ctx -> fireWorldLeave(ctx, module)))
                        .then(literal("dimension_change")
                                .then(argument("dimension", StringArgumentType.greedyString())
                                        .executes(ctx -> fireDimensionChange(ctx, module))))
                        .then(literal("chat")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> fireChat(ctx, module))))
                        .then(literal("screen_change")
                                .executes(ctx -> fireScreenChange(ctx, module, "InventoryScreen"))
                                .then(argument("screen", StringArgumentType.greedyString())
                                        .executes(ctx -> fireScreenChange(ctx, module, StringArgumentType.getString(ctx, "screen")))))
                        .then(literal("weather_change")
                                .executes(ctx -> fireWeatherChange(ctx, module, false, true, false, false))
                                .then(argument("toRaining", StringArgumentType.word())
                                        .then(argument("toThundering", StringArgumentType.word())
                                                .executes(ctx -> fireWeatherChange(
                                                        ctx,
                                                        module,
                                                        false,
                                                        parseBooleanWord(StringArgumentType.getString(ctx, "toRaining"), true),
                                                        false,
                                                        parseBooleanWord(StringArgumentType.getString(ctx, "toThundering"), false)
                                                )))))
                        .then(literal("hotbar_change")
                                .executes(ctx -> fireHotbarChange(ctx, module, 1))
                                .then(argument("slot", IntegerArgumentType.integer(0, 8))
                                        .executes(ctx -> fireHotbarChange(ctx, module, IntegerArgumentType.getInteger(ctx, "slot")))))
                        .then(literal("held_item_change")
                                .executes(ctx -> fireHeldItemChange(ctx, module, "minecraft:diamond_sword"))
                                .then(argument("itemId", StringArgumentType.greedyString())
                                        .executes(ctx -> fireHeldItemChange(ctx, module, StringArgumentType.getString(ctx, "itemId")))))
                        .then(literal("health_change")
                                .executes(ctx -> fireHealthChange(ctx, module, 20.0F, 16.0F))
                                .then(argument("toHealth", FloatArgumentType.floatArg(0.0F))
                                        .executes(ctx -> fireHealthChange(ctx, module, 20.0F, FloatArgumentType.getFloat(ctx, "toHealth")))))
                        .then(literal("food_change")
                                .executes(ctx -> fireFoodChange(ctx, module, 20, 16))
                                .then(argument("toFood", IntegerArgumentType.integer(0, 20))
                                        .executes(ctx -> fireFoodChange(ctx, module, 20, IntegerArgumentType.getInteger(ctx, "toFood")))))
                        .then(literal("level_change")
                                .executes(ctx -> fireLevelChange(ctx, module, 10, 11, 0.25F, 0.05F))
                                .then(argument("toLevel", IntegerArgumentType.integer(0))
                                        .executes(ctx -> fireLevelChange(ctx, module, 10, IntegerArgumentType.getInteger(ctx, "toLevel"), 0.25F, 0.05F))))
                        .then(literal("death")
                                .executes(ctx -> fireDeath(ctx, module, 0.0F))
                                .then(argument("health", FloatArgumentType.floatArg(0.0F, 1.0F))
                                        .executes(ctx -> fireDeath(ctx, module, FloatArgumentType.getFloat(ctx, "health"))))));
        dispatcher.register(root);
    }

    private static int openUi(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        var client = context.getSource().getClient();
        client.execute(() -> module.openScreen(client.currentScreen));
        context.getSource().sendFeedback(Text.literal("[automation] opened rule editor"));
        return 1;
    }

    private static int reload(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.engine().reload();
        context.getSource().sendFeedback(Text.literal("[automation] reloaded rules from disk"));
        return 1;
    }

    private static int save(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.engine().save();
        context.getSource().sendFeedback(Text.literal("[automation] saved rules"));
        return 1;
    }

    private static int listRules(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        List<AutomationRule> rules = module.engine().getRules();
        if (rules.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("[automation] no rules configured"));
            return 1;
        }

        context.getSource().sendFeedback(Text.literal("[automation] rules:"));
        for (AutomationRule rule : rules) {
            AutomationEngine.RuleRuntimeSnapshot snapshot = module.engine().getRuntimeSnapshot(rule.id);
            String last = snapshot.lastExecution() == null
                    ? "never"
                    : (snapshot.lastExecution().success() ? "ok" : "fail") + " - " + snapshot.lastExecution().message();
            context.getSource().sendFeedback(Text.literal(" - " + rule.id
                    + " [" + (rule.enabled ? "on" : "off") + "] "
                    + rule.eventType.name() + " prio=" + rule.priority
                    + " last=" + last));
        }
        return 1;
    }

    private static int printConfigPath(CommandContext<FabricClientCommandSource> context) {
        Path path = DataHandler.configPath();
        context.getSource().sendFeedback(Text.literal("[automation] config: " + path.toAbsolutePath()));
        return 1;
    }

    private static int setRuleEnabled(CommandContext<FabricClientCommandSource> context, AutomationModule module, boolean enabled) {
        String id = StringArgumentType.getString(context, "id");
        AutomationRule rule = module.engine().findRule(id);
        if (rule == null) {
            context.getSource().sendError(Text.literal("[automation] unknown rule: " + id));
            return 0;
        }
        rule.enabled = enabled;
        module.engine().upsertRule(rule);
        context.getSource().sendFeedback(Text.literal("[automation] rule '" + id + "' " + (enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int printLogs(CommandContext<FabricClientCommandSource> context, AutomationModule module, int count) {
        List<AutomationEngine.LogEntry> logs = module.engine().getDiagnostics(count);
        if (logs.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("[automation] no diagnostics yet"));
            return 1;
        }
        context.getSource().sendFeedback(Text.literal("[automation] diagnostics:"));
        for (AutomationEngine.LogEntry log : logs) {
            String prefix = log.ruleId() == null ? "" : ("[" + log.ruleId() + "] ");
            context.getSource().sendFeedback(Text.literal(" - " + log.level() + " " + prefix + log.message()));
        }
        return 1;
    }

    private static int fireTick(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.fireTickTest();
        context.getSource().sendFeedback(Text.literal("[automation] fired TICK_INTERVAL"));
        return 1;
    }

    private static int fireMove(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.firePlayerMoveTest();
        context.getSource().sendFeedback(Text.literal("[automation] fired PLAYER_MOVE"));
        return 1;
    }

    private static int fireWorldJoin(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.fireWorldJoinTest();
        context.getSource().sendFeedback(Text.literal("[automation] fired WORLD_JOIN"));
        return 1;
    }

    private static int fireWorldLeave(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.fireWorldLeaveTest();
        context.getSource().sendFeedback(Text.literal("[automation] fired WORLD_LEAVE"));
        return 1;
    }

    private static int fireDimensionChange(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String dimension = StringArgumentType.getString(context, "dimension");
        module.fireDimensionChangeTest(dimension);
        context.getSource().sendFeedback(Text.literal("[automation] fired DIMENSION_CHANGE -> " + dimension));
        return 1;
    }

    private static int fireChat(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String message = StringArgumentType.getString(context, "message");
        module.fireChatTest(message);
        context.getSource().sendFeedback(Text.literal("[automation] fired CHAT_RECEIVED_REGEX"));
        return 1;
    }

    private static int fireScreenChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, String screen) {
        module.fireScreenChangeTest(screen);
        context.getSource().sendFeedback(Text.literal("[automation] fired SCREEN_CHANGED -> " + screen));
        return 1;
    }

    private static int fireWeatherChange(CommandContext<FabricClientCommandSource> context,
                                         AutomationModule module,
                                         boolean fromRaining,
                                         boolean toRaining,
                                         boolean fromThundering,
                                         boolean toThundering) {
        module.fireWeatherChangeTest(fromRaining, toRaining, fromThundering, toThundering);
        context.getSource().sendFeedback(Text.literal("[automation] fired WEATHER_CHANGED"));
        return 1;
    }

    private static int fireHotbarChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, int slot) {
        module.fireHotbarSlotChangeTest(slot);
        context.getSource().sendFeedback(Text.literal("[automation] fired HOTBAR_SLOT_CHANGED -> " + slot));
        return 1;
    }

    private static int fireHeldItemChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, String itemId) {
        module.fireHeldItemChangeTest(itemId);
        context.getSource().sendFeedback(Text.literal("[automation] fired HELD_ITEM_CHANGED -> " + itemId));
        return 1;
    }

    private static int fireHealthChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, float fromHealth, float toHealth) {
        module.firePlayerHealthChangeTest(fromHealth, toHealth);
        context.getSource().sendFeedback(Text.literal("[automation] fired PLAYER_HEALTH_CHANGED -> " + toHealth));
        return 1;
    }

    private static int fireFoodChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, int fromFood, int toFood) {
        module.firePlayerFoodChangeTest(fromFood, toFood);
        context.getSource().sendFeedback(Text.literal("[automation] fired PLAYER_FOOD_CHANGED -> " + toFood));
        return 1;
    }

    private static int fireLevelChange(CommandContext<FabricClientCommandSource> context,
                                       AutomationModule module,
                                       int fromLevel,
                                       int toLevel,
                                       float fromProgress,
                                       float toProgress) {
        module.firePlayerLevelChangeTest(fromLevel, toLevel, fromProgress, toProgress);
        context.getSource().sendFeedback(Text.literal("[automation] fired PLAYER_LEVEL_CHANGED -> " + toLevel));
        return 1;
    }

    private static int fireDeath(CommandContext<FabricClientCommandSource> context, AutomationModule module, float health) {
        module.firePlayerDeathTest(health);
        context.getSource().sendFeedback(Text.literal("[automation] fired PLAYER_DEATH"));
        return 1;
    }

    private static boolean parseBooleanWord(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "true", "yes", "on" -> true;
            case "0", "false", "no", "off" -> false;
            default -> fallback;
        };
    }
}
