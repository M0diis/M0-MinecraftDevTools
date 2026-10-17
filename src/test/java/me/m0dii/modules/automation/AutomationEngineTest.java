package me.m0dii.modules.automation;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class AutomationEngineTest {
    @Test
    void ruleMatchingDispatchesRegexEvent() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "chat_rule";
        rule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        rule.actions = List.of(AutomationRule.Action.command("/say hi"));
        rule.eventFilters = List.of(AutomationRule.EventFilter.regex("message", ".*joined.*"));
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.CHAT_RECEIVED_REGEX,
                1_000L,
                20L,
                Map.of("message", "player joined the game")
        ));
        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.CHAT_RECEIVED_REGEX,
                2_000L,
                40L,
                Map.of("message", "nothing to match")
        ));

        assertEquals(List.of("chat_rule@CHAT_RECEIVED_REGEX"), executor.executions);
    }

    @Test
    void cooldownBlocksRepeatedExecutionUntilWindowExpires() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "cooldown_rule";
        rule.eventType = AutomationEventType.WORLD_JOIN;
        rule.actions = List.of(AutomationRule.Action.command("/say hi"));
        rule.cooldownMs = 1_000L;
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(AutomationEvent.of(AutomationEventType.WORLD_JOIN, 1_000L, 1L));
        engine.onAutomationEvent(AutomationEvent.of(AutomationEventType.WORLD_JOIN, 1_500L, 2L));
        engine.onAutomationEvent(AutomationEvent.of(AutomationEventType.WORLD_JOIN, 2_100L, 3L));

        assertEquals(List.of(
                "cooldown_rule@WORLD_JOIN",
                "cooldown_rule@WORLD_JOIN"
        ), executor.executions);
    }

    @Test
    void screenChangeRuleDispatchesForMatchingTargetScreen() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "screen_rule";
        rule.eventType = AutomationEventType.SCREEN_CHANGED;
        rule.actions = List.of(AutomationRule.Action.command("/say ui"));

        AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
        filter.field = "toScreen";
        filter.operator = AutomationRule.Operator.EQUALS;
        filter.value = "InventoryScreen";
        rule.eventFilters = List.of(filter);

        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.SCREEN_CHANGED,
                1_000L,
                20L,
                Map.of("fromScreen", "", "toScreen", "ChatScreen")
        ));
        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.SCREEN_CHANGED,
                2_000L,
                40L,
                Map.of("fromScreen", "", "toScreen", "InventoryScreen")
        ));

        assertEquals(List.of("screen_rule@SCREEN_CHANGED"), executor.executions);
    }

    @Test
    void placeholderConditionSourceResolvesSelectedToken() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = getAutomationEngine(executor);

        AutomationRule rule = new AutomationRule();
        rule.id = "placeholder_rule";
        rule.eventType = AutomationEventType.WORLD_JOIN;
        rule.actions = List.of(AutomationRule.Action.command("/say ok"));

        AutomationRule.Condition condition = new AutomationRule.Condition();
        condition.source = AutomationRule.ConditionSource.PLACEHOLDER;
        condition.field = "player.name";
        condition.operator = AutomationRule.Operator.EQUALS;
        condition.value = "Steve";
        rule.conditions = List.of(condition);

        engine.setRulesInMemory(List.of(rule));
        engine.onAutomationEvent(AutomationEvent.of(AutomationEventType.WORLD_JOIN, 1_000L, 1L));

        assertEquals(List.of("placeholder_rule@WORLD_JOIN"), executor.executions);
    }

    @Test
    void worldLeaveRuleDispatchesWhenDimensionMatches() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "leave_rule";
        rule.eventType = AutomationEventType.WORLD_LEAVE;
        rule.actions = List.of(AutomationRule.Action.command("/say bye"));
        rule.eventFilters = List.of(AutomationRule.EventFilter.regex("fromDimension", "minecraft:overworld"));
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.WORLD_LEAVE,
                1_000L,
                20L,
                Map.of("fromDimension", "minecraft:overworld")
        ));

        assertEquals(List.of("leave_rule@WORLD_LEAVE"), executor.executions);
    }

    @Test
    void playerDeathRuleDispatchesWhenHealthDropsToZero() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "death_rule";
        rule.eventType = AutomationEventType.PLAYER_DEATH;
        rule.actions = List.of(AutomationRule.Action.command("/say respawn"));

        AutomationRule.EventFilter filter = new AutomationRule.EventFilter();
        filter.field = "health";
        filter.operator = AutomationRule.Operator.LESS_OR_EQUAL;
        filter.value = "0";
        rule.eventFilters = List.of(filter);

        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.PLAYER_DEATH,
                1_000L,
                20L,
                Map.of("health", 0.0F)
        ));

        assertEquals(List.of("death_rule@PLAYER_DEATH"), executor.executions);
    }

    private static @NonNull AutomationEngine getAutomationEngine(RecordingExecutor executor) {
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, new AutomationEngine.ContextSnapshotProvider() {
            @Override
            public @NonNull Map<String, Object> snapshot(AutomationRule.@NonNull ConditionSource source, @NonNull AutomationEvent event) {
                return Map.of();
            }

            @Override
            public Object resolve(AutomationRule.@NonNull ConditionSource source, @NonNull String field, @NonNull AutomationEvent event) {
                if (source == AutomationRule.ConditionSource.PLACEHOLDER && "player.name".equals(field)) {
                    return "Steve";
                }
                return null;
            }
        });
        engine.configureInMemory(10, 10_000L, 10, 10_000L);
        return engine;
    }

    @Test
    void regexCaptureGroupsAreExposedToActions() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "tpa_rule";
        rule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        rule.actions = List.of(AutomationRule.Action.command("/tpaccept {event.group1}"));
        rule.eventFilters = List.of(AutomationRule.EventFilter.regex("message", "(?<who>\\w+) has requested to teleport"));
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.CHAT_RECEIVED_REGEX, 1_000L, 20L,
                Map.of("message", "[Server] Steve has requested to teleport to you")));

        assertEquals(List.of("tpa_rule@CHAT_RECEIVED_REGEX"), executor.executions);
        assertEquals("Steve", executor.lastEvent.attribute("group1"));
        assertEquals("Steve", executor.lastEvent.attribute("group.who"));
        assertEquals("Steve has requested to teleport", executor.lastEvent.attribute("group0"));
    }

    @Test
    void invalidRegexNeverThrowsAndNeverMatches() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "broken_rule";
        rule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        rule.actions = List.of(AutomationRule.Action.command("/say hi"));
        rule.eventFilters = List.of(AutomationRule.EventFilter.regex("message", "(unclosed"));
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.CHAT_RECEIVED_REGEX, 1_000L, 20L, Map.of("message", "(unclosed")));

        assertEquals(List.of(), executor.executions);
    }

    @Test
    void regexHonoursIgnoreCase() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);

        AutomationRule rule = new AutomationRule();
        rule.id = "case_rule";
        rule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        rule.actions = List.of(AutomationRule.Action.command("/say hi"));
        AutomationRule.EventFilter filter = AutomationRule.EventFilter.regex("message", "joined the game");
        filter.ignoreCase = true;
        rule.eventFilters = List.of(filter);
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(
                AutomationEventType.CHAT_RECEIVED_REGEX, 1_000L, 20L, Map.of("message", "Alex JOINED THE GAME")));

        assertEquals(List.of("case_rule@CHAT_RECEIVED_REGEX"), executor.executions);
    }

    @Test
    void explainReportsChecksGroupsAndExpandedActions() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        AutomationRule rule = new AutomationRule();
        rule.id = "explain_rule";
        rule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        rule.eventFilters = List.of(AutomationRule.EventFilter.regex("message", "(\\w+) says hi"));
        rule.actions = List.of(AutomationRule.Action.command("/msg {event.group1} hello"));

        List<String> lines = engine.explain(rule, new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, 1L, 1L,
                Map.of("message", "Alex says hi")));
        String text = String.join("\n", lines);
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("✔ filter message REGEX"), text);
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("would run"), text);
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("SEND_CLIENT_COMMAND /msg Alex hello"), text);
        assertEquals(List.of(), executor.executions);

        List<String> miss = engine.explain(rule, new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, 1L, 1L,
                Map.of("message", "nothing")));
        org.junit.jupiter.api.Assertions.assertTrue(String.join("\n", miss).contains("would NOT run"));
    }

    @Test
    void mentionTemplateIgnoresOwnMessages() {
        RecordingExecutor executor = new RecordingExecutor();
        AutomationEngine engine = new AutomationEngine(EventBus.getInstance(), executor, (source, event) -> Map.of());
        engine.configureInMemory(10, 10_000L, 10, 10_000L);
        AutomationRule rule = AutomationTemplates.create(AutomationTemplates.find("mention_ping"),
                new AutomationTemplates.Context("Steve"), List.of());
        rule.cooldownMs = 0L;
        engine.setRulesInMemory(List.of(rule));

        engine.onAutomationEvent(new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, 1_000L, 1L,
                Map.of("message", "<Steve> hello there")));
        engine.onAutomationEvent(new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, 2_000L, 2L,
                Map.of("message", "<Alex> hey steve, come here")));
        engine.onAutomationEvent(new AutomationEvent(AutomationEventType.CHAT_RECEIVED_REGEX, 3_000L, 3L,
                Map.of("message", "<Alex> Steven is not you")));

        assertEquals(List.of(rule.id + "@CHAT_RECEIVED_REGEX"), executor.executions);
    }

    @Test
    void templatesGetUniqueIds() {
        AutomationTemplates.Template template = AutomationTemplates.find("death_coordinates");
        AutomationRule first = AutomationTemplates.create(template, new AutomationTemplates.Context(null), List.of());
        AutomationRule second = AutomationTemplates.create(template, new AutomationTemplates.Context(null), List.of(first));
        assertEquals("death_coordinates", first.id);
        assertEquals("death_coordinates_2", second.id);
        org.junit.jupiter.api.Assertions.assertFalse(AutomationTemplates.create(AutomationTemplates.find("tpa_accept"),
                new AutomationTemplates.Context(null), List.of()).enabled);
    }

    private static final class RecordingExecutor extends ActionExecutor {
        private final List<String> executions = new ArrayList<>();
        private AutomationEvent lastEvent;

        private RecordingExecutor() {
            super(new NoOpBridge());
        }

        @Override
        public ActionResult execute(@NonNull AutomationRule rule, @NonNull AutomationEvent event) {
            executions.add(rule.id + "@" + event.type().name());
            lastEvent = event;
            return ActionResult.ok("executed");
        }
    }

    private static final class NoOpBridge implements ActionExecutor.Bridge {
        @Override
        public boolean runMacro(@NonNull String macroId) {
            return true;
        }

        @Override
        public ActionExecutor.ActionResult runScript(@NonNull String scriptFile, @NonNull Map<String, Object> context) {
            return ActionExecutor.ActionResult.ok("ok");
        }

        @Override
        public boolean sendClientCommand(@NonNull String command) {
            return true;
        }

        @Override
        public boolean toggleModule(@NonNull String moduleId, Boolean enabledState) {
            return true;
        }

        @Override
        public net.minecraft.client.Minecraft client() {
            return null;
        }

        @Override
        public net.minecraft.client.player.LocalPlayer player() {
            return null;
        }

        @Override
        public net.minecraft.client.multiplayer.ClientLevel world() {
            return null;
        }
    }
}
