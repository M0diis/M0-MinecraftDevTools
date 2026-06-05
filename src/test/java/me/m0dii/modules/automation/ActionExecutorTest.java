package me.m0dii.modules.automation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActionExecutorTest {
    @Test
    void dispatchesAllSupportedActionTypes() {
        RecordingBridge bridge = new RecordingBridge();
        ActionExecutor executor = new ActionExecutor(bridge);

        AutomationRule rule = new AutomationRule();
        rule.actions = List.of(
                AutomationRule.Action.macro("macro_{event.clientTick}"),
                AutomationRule.Action.script("{event.scriptFile}"),
                AutomationRule.Action.command("/say {event.message}"),
                AutomationRule.Action.toggleModule("{event.moduleId}")
        );
        rule.actions.get(3).argument = "{event.state}";

        AutomationEvent event = new AutomationEvent(
                AutomationEventType.TICK_INTERVAL,
                1_000L,
                20L,
                Map.of(
                        "tick", 20L,
                        "scriptFile", "hello.kts",
                        "message", "hi",
                        "moduleId", "xray",
                        "state", "off"
                )
        );

        ActionExecutor.ActionResult result = executor.execute(rule, event);

        assertTrue(result.success());
        assertEquals(List.of(
                "macro:macro_20",
                "script:hello.kts",
                "command:/say hi",
                "toggle:xray:false"
        ), bridge.calls);
        assertTrue(bridge.lastScriptContext.containsKey("event"));
        assertTrue(bridge.lastScriptContext.containsKey("client"));
        assertTrue(bridge.lastScriptContext.containsKey("player"));
        assertTrue(bridge.lastScriptContext.containsKey("world"));
        assertSame(event, bridge.lastScriptContext.get("event"));
    }

    private static final class RecordingBridge implements ActionExecutor.Bridge {
        private final List<String> calls = new ArrayList<>();
        private Map<String, Object> lastScriptContext = Map.of();

        @Override
        public boolean runMacro(String macroId) {
            calls.add("macro:" + macroId);
            return true;
        }

        @Override
        public ActionExecutor.ActionResult runScript(String scriptFile, Map<String, Object> context) {
            calls.add("script:" + scriptFile);
            lastScriptContext = context;
            return ActionExecutor.ActionResult.ok("script ok");
        }

        @Override
        public boolean sendClientCommand(String command) {
            calls.add("command:" + command);
            return true;
        }

        @Override
        public boolean toggleModule(String moduleId, Boolean enabledState) {
            calls.add("toggle:" + moduleId + ":" + enabledState);
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
