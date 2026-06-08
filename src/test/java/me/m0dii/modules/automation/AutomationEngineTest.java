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

    private static final class RecordingExecutor extends ActionExecutor {
        private final List<String> executions = new ArrayList<>();

        private RecordingExecutor() {
            super(new NoOpBridge());
        }

        @Override
        public ActionResult execute(AutomationRule rule, AutomationEvent event) {
            executions.add(rule.id + "@" + event.type().name());
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
        public net.minecraft.client.MinecraftClient client() {
            return null;
        }

        @Override
        public net.minecraft.client.network.ClientPlayerEntity player() {
            return null;
        }

        @Override
        public net.minecraft.client.world.ClientWorld world() {
            return null;
        }
    }
}
