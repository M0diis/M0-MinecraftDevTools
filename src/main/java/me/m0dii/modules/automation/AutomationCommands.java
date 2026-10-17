package me.m0dii.modules.automation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

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
                .then(literal("test")
                        .then(argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    module.engine().getRules().forEach(rule -> builder.suggest(rule.id));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> testRule(ctx, module, null))
                                .then(argument("input", StringArgumentType.greedyString())
                                        .executes(ctx -> testRule(ctx, module, StringArgumentType.getString(ctx, "input"))))))
                .then(literal("folder")
                        .then(argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    module.engine().getRules().forEach(rule -> builder.suggest(rule.id));
                                    return builder.buildFuture();
                                })
                                .then(argument("folder", StringArgumentType.greedyString())
                                        .executes(ctx -> setFolder(ctx, module)))))
                .then(literal("folder-on")
                        .then(argument("folder", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> suggestFolders(module, builder))
                                .executes(ctx -> setFolderEnabled(ctx, module, true))))
                .then(literal("folder-off")
                        .then(argument("folder", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> suggestFolders(module, builder))
                                .executes(ctx -> setFolderEnabled(ctx, module, false))))
                .then(literal("export")
                        .executes(ctx -> exportRules(ctx, module, "all"))
                        .then(argument("what", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("all");
                                    module.engine().getRules().forEach(rule -> builder.suggest(rule.id));
                                    folders(module).forEach(folder -> builder.suggest("folder:" + folder));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> exportRules(ctx, module, StringArgumentType.getString(ctx, "what")))))
                .then(literal("import").executes(ctx -> importRules(ctx, module)))
                .then(literal("template")
                        .executes(AutomationCommands::listTemplates)
                        .then(argument("template", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    AutomationTemplates.ALL.forEach(t -> builder.suggest(t.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> addTemplate(ctx, module))))
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

    private static java.util.Set<String> folders(AutomationModule module) {
        java.util.Set<String> folders = new java.util.TreeSet<>();
        for (AutomationRule rule : module.engine().getRules()) {
            if (rule.group != null && !rule.group.isBlank()) {
                folders.add(rule.group);
            }
        }
        return folders;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestFolders(
            AutomationModule module, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        folders(module).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int setFolder(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String id = StringArgumentType.getString(context, "id");
        String folder = StringArgumentType.getString(context, "folder").trim();
        AutomationRule rule = module.engine().findRule(id);
        if (rule == null) {
            context.getSource().sendError(Component.literal("[automation] no rule with id '" + id + "'"));
            return 0;
        }
        rule.group = "-".equals(folder) || "none".equalsIgnoreCase(folder) ? "" : folder;
        module.engine().upsertRule(rule);
        context.getSource().sendFeedback(Component.literal("[automation] '" + id + "' is now "
                + (rule.group.isEmpty() ? "in no folder" : "in folder '" + rule.group + "'")));
        return 1;
    }

    private static int setFolderEnabled(CommandContext<FabricClientCommandSource> context, AutomationModule module, boolean enabled) {
        String folder = StringArgumentType.getString(context, "folder").trim();
        int changed = 0;
        for (AutomationRule rule : module.engine().getRules()) {
            if (folder.equalsIgnoreCase(rule.group) && rule.enabled != enabled) {
                rule.enabled = enabled;
                module.engine().upsertRule(rule);
                changed++;
            }
        }
        context.getSource().sendFeedback(Component.literal("[automation] " + (enabled ? "enabled " : "disabled ")
                + changed + " rule(s) in folder '" + folder + "'"));
        return 1;
    }

    private static int exportRules(CommandContext<FabricClientCommandSource> context, AutomationModule module, String what) {
        String selector = what == null ? "all" : what.trim();
        List<AutomationRule> chosen = new java.util.ArrayList<>();
        for (AutomationRule rule : module.engine().getRules()) {
            boolean match = selector.equalsIgnoreCase("all")
                    || rule.id.equalsIgnoreCase(selector)
                    || (selector.regionMatches(true, 0, "folder:", 0, 7) && selector.substring(7).trim().equalsIgnoreCase(rule.group));
            if (match) {
                chosen.add(rule);
            }
        }
        if (chosen.isEmpty()) {
            context.getSource().sendError(Component.literal("[automation] nothing matches '" + selector + "' (use all, a rule id or folder:<name>)"));
            return 0;
        }
        context.getSource().getClient().keyboardHandler.setClipboard(AutomationTransfer.exportJson(chosen));
        long scripts = chosen.stream().flatMap(r -> r.actions.stream()).filter(a -> a.type == AutomationActionType.RUN_SCRIPT).count();
        context.getSource().sendFeedback(Component.literal("[automation] copied " + chosen.size() + " rule(s) to the clipboard"
                + (scripts > 0 ? ". They use " + scripts + " script action(s); share those script files too." : ".")));
        return 1;
    }

    private static int importRules(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String clipboard = context.getSource().getClient().keyboardHandler.getClipboard();
        java.util.Set<String> existing = new java.util.HashSet<>();
        module.engine().getRules().forEach(rule -> existing.add(rule.id));
        AutomationTransfer.ImportResult result;
        try {
            result = AutomationTransfer.parse(clipboard, existing);
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Component.literal("[automation] " + e.getMessage()));
            return 0;
        }
        for (AutomationRule rule : result.rules()) {
            module.engine().upsertRule(rule);
        }
        context.getSource().sendFeedback(Component.literal("[automation] imported " + result.rules().size() + " rule(s)."));
        for (String note : result.notes()) {
            context.getSource().sendFeedback(Component.literal("[automation]  - " + note));
        }
        return 1;
    }

    private static int testRule(CommandContext<FabricClientCommandSource> context, AutomationModule module, String input) {
        String id = StringArgumentType.getString(context, "id");
        AutomationRule rule = module.engine().findRule(id);
        if (rule == null) {
            context.getSource().sendError(Component.literal("[automation] no rule with id '" + id + "' (see /automation list)"));
            return 0;
        }
        AutomationEvent event = module.sampleEvent(rule.eventType, input);
        for (String line : module.engine().explain(rule, event)) {
            context.getSource().sendFeedback(Component.literal("[test] " + line));
        }
        return 1;
    }

    private static int listTemplates(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("[automation] templates (add with /automation template <id>):"));
        for (AutomationTemplates.Template template : AutomationTemplates.ALL) {
            context.getSource().sendFeedback(Component.literal(" - " + template.id() + ": " + template.title()));
        }
        return 1;
    }

    private static int addTemplate(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String id = StringArgumentType.getString(context, "template");
        AutomationTemplates.Template template = AutomationTemplates.find(id);
        if (template == null) {
            context.getSource().sendError(Component.literal("[automation] unknown template '" + id + "' (see /automation template)"));
            return 0;
        }
        var player = context.getSource().getPlayer();
        String playerName = player == null ? null : player.getName().getString();
        AutomationRule rule = AutomationTemplates.create(template, new AutomationTemplates.Context(playerName), module.engine().getRules());
        module.engine().upsertRule(rule);
        context.getSource().sendFeedback(Component.literal("[automation] added rule '" + rule.id + "' ("
                + (rule.enabled ? "on" : "OFF - check it in /automation ui, then /automation enable " + rule.id)
                + "). Try it with /automation test " + rule.id));
        return 1;
    }

    private static int openUi(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        var client = context.getSource().getClient();
        client.execute(() -> module.openScreen(client.gui.screen()));
        context.getSource().sendFeedback(Component.literal("[automation] opened rule editor"));
        return 1;
    }

    private static int reload(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.engine().reload();
        context.getSource().sendFeedback(Component.literal("[automation] reloaded rules from disk"));
        return 1;
    }

    private static int save(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.engine().save();
        context.getSource().sendFeedback(Component.literal("[automation] saved rules"));
        return 1;
    }

    private static int listRules(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        List<AutomationRule> rules = module.engine().getRules();
        if (rules.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("[automation] no rules configured"));
            return 1;
        }

        context.getSource().sendFeedback(Component.literal("[automation] rules:"));
        java.util.Map<String, List<AutomationRule>> byFolder = new java.util.TreeMap<>();
        for (AutomationRule rule : rules) {
            String folder = rule.group == null || rule.group.isBlank() ? "" : rule.group;
            byFolder.computeIfAbsent(folder, ignored -> new java.util.ArrayList<>()).add(rule);
        }
        for (java.util.Map.Entry<String, List<AutomationRule>> folder : byFolder.entrySet()) {
            String indent = " ";
            if (!folder.getKey().isEmpty()) {
                context.getSource().sendFeedback(Component.literal(" [" + folder.getKey() + "]"));
                indent = "   ";
            }
            for (AutomationRule rule : folder.getValue()) {
                AutomationEngine.RuleRuntimeSnapshot snapshot = module.engine().getRuntimeSnapshot(rule.id);
                String last = snapshot.lastExecution() == null
                        ? "never"
                        : (snapshot.lastExecution().success() ? "ok" : "fail") + " - " + snapshot.lastExecution().message();
                context.getSource().sendFeedback(Component.literal(indent + "- " + rule.id
                        + " [" + (rule.enabled ? "on" : "off") + "] "
                        + rule.eventType.name() + " prio=" + rule.priority
                        + " fired=" + module.engine().fireCount(rule.id)
                        + " last=" + last));
            }
        }
        return 1;
    }

    private static int printConfigPath(CommandContext<FabricClientCommandSource> context) {
        Path path = DataHandler.configPath();
        context.getSource().sendFeedback(Component.literal("[automation] config: " + path.toAbsolutePath()));
        return 1;
    }

    private static int setRuleEnabled(CommandContext<FabricClientCommandSource> context, AutomationModule module, boolean enabled) {
        String id = StringArgumentType.getString(context, "id");
        AutomationRule rule = module.engine().findRule(id);
        if (rule == null) {
            context.getSource().sendError(Component.literal("[automation] unknown rule: " + id));
            return 0;
        }
        rule.enabled = enabled;
        module.engine().upsertRule(rule);
        context.getSource().sendFeedback(Component.literal("[automation] rule '" + id + "' " + (enabled ? "enabled" : "disabled")));
        return 1;
    }

    private static int printLogs(CommandContext<FabricClientCommandSource> context, AutomationModule module, int count) {
        List<AutomationEngine.LogEntry> logs = module.engine().getDiagnostics(count);
        if (logs.isEmpty()) {
            context.getSource().sendFeedback(Component.literal("[automation] no diagnostics yet"));
            return 1;
        }
        context.getSource().sendFeedback(Component.literal("[automation] diagnostics:"));
        for (AutomationEngine.LogEntry log : logs) {
            String prefix = log.ruleId() == null ? "" : ("[" + log.ruleId() + "] ");
            context.getSource().sendFeedback(Component.literal(" - " + log.level() + " " + prefix + log.message()));
        }
        return 1;
    }

    private static int fireTick(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.fireTickTest();
        context.getSource().sendFeedback(Component.literal("[automation] fired TICK_INTERVAL"));
        return 1;
    }

    private static int fireMove(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.firePlayerMoveTest();
        context.getSource().sendFeedback(Component.literal("[automation] fired PLAYER_MOVE"));
        return 1;
    }

    private static int fireWorldJoin(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.fireWorldJoinTest();
        context.getSource().sendFeedback(Component.literal("[automation] fired WORLD_JOIN"));
        return 1;
    }

    private static int fireWorldLeave(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        module.fireWorldLeaveTest();
        context.getSource().sendFeedback(Component.literal("[automation] fired WORLD_LEAVE"));
        return 1;
    }

    private static int fireDimensionChange(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String dimension = StringArgumentType.getString(context, "dimension");
        module.fireDimensionChangeTest(dimension);
        context.getSource().sendFeedback(Component.literal("[automation] fired DIMENSION_CHANGE -> " + dimension));
        return 1;
    }

    private static int fireChat(CommandContext<FabricClientCommandSource> context, AutomationModule module) {
        String message = StringArgumentType.getString(context, "message");
        module.fireChatTest(message);
        context.getSource().sendFeedback(Component.literal("[automation] fired CHAT_RECEIVED_REGEX"));
        return 1;
    }

    private static int fireScreenChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, String screen) {
        module.fireScreenChangeTest(screen);
        context.getSource().sendFeedback(Component.literal("[automation] fired SCREEN_CHANGED -> " + screen));
        return 1;
    }

    private static int fireWeatherChange(CommandContext<FabricClientCommandSource> context,
                                         AutomationModule module,
                                         boolean fromRaining,
                                         boolean toRaining,
                                         boolean fromThundering,
                                         boolean toThundering) {
        module.fireWeatherChangeTest(fromRaining, toRaining, fromThundering, toThundering);
        context.getSource().sendFeedback(Component.literal("[automation] fired WEATHER_CHANGED"));
        return 1;
    }

    private static int fireHotbarChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, int slot) {
        module.fireHotbarSlotChangeTest(slot);
        context.getSource().sendFeedback(Component.literal("[automation] fired HOTBAR_SLOT_CHANGED -> " + slot));
        return 1;
    }

    private static int fireHeldItemChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, String itemId) {
        module.fireHeldItemChangeTest(itemId);
        context.getSource().sendFeedback(Component.literal("[automation] fired HELD_ITEM_CHANGED -> " + itemId));
        return 1;
    }

    private static int fireHealthChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, float fromHealth, float toHealth) {
        module.firePlayerHealthChangeTest(fromHealth, toHealth);
        context.getSource().sendFeedback(Component.literal("[automation] fired PLAYER_HEALTH_CHANGED -> " + toHealth));
        return 1;
    }

    private static int fireFoodChange(CommandContext<FabricClientCommandSource> context, AutomationModule module, int fromFood, int toFood) {
        module.firePlayerFoodChangeTest(fromFood, toFood);
        context.getSource().sendFeedback(Component.literal("[automation] fired PLAYER_FOOD_CHANGED -> " + toFood));
        return 1;
    }

    private static int fireLevelChange(CommandContext<FabricClientCommandSource> context,
                                       AutomationModule module,
                                       int fromLevel,
                                       int toLevel,
                                       float fromProgress,
                                       float toProgress) {
        module.firePlayerLevelChangeTest(fromLevel, toLevel, fromProgress, toProgress);
        context.getSource().sendFeedback(Component.literal("[automation] fired PLAYER_LEVEL_CHANGED -> " + toLevel));
        return 1;
    }

    private static int fireDeath(CommandContext<FabricClientCommandSource> context, AutomationModule module, float health) {
        module.firePlayerDeathTest(health);
        context.getSource().sendFeedback(Component.literal("[automation] fired PLAYER_DEATH"));
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
