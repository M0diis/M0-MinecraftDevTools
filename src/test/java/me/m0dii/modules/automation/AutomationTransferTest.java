package me.m0dii.modules.automation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AutomationTransferTest {
    private static AutomationRule rule(String id, AutomationRule.Action action) {
        AutomationRule rule = new AutomationRule();
        rule.id = id;
        rule.name = "Rule " + id;
        rule.group = "pvp";
        rule.eventType = AutomationEventType.CHAT_RECEIVED_REGEX;
        rule.eventFilters.add(AutomationRule.EventFilter.regex("message", "hi (\\w+)"));
        rule.actions.add(action);
        return rule;
    }

    @Test
    void roundTripKeepsRulesAndFolders() {
        String json = AutomationTransfer.exportJson(List.of(rule("greet", AutomationRule.Action.message("hi {event.group1}", ""))));
        AutomationTransfer.ImportResult result = AutomationTransfer.parse(json, Set.of());
        assertEquals(1, result.rules().size());
        AutomationRule imported = result.rules().getFirst();
        assertEquals("greet", imported.id);
        assertEquals("pvp", imported.group);
        assertEquals("hi (\\w+)", imported.eventFilters.getFirst().value);
        assertTrue(imported.enabled, "message-only rules stay enabled");
    }

    @Test
    void riskyRulesAreImportedDisabledAndClashingIdsRenamed() {
        String json = AutomationTransfer.exportJson(List.of(rule("greet", AutomationRule.Action.command("/say hi"))));
        AutomationTransfer.ImportResult result = AutomationTransfer.parse(json, Set.of("greet"));
        AutomationRule imported = result.rules().getFirst();
        assertEquals("greet_2", imported.id);
        assertFalse(imported.enabled);
        assertTrue(String.join(" ", result.notes()).contains("sends commands"), result.notes().toString());
    }

    @Test
    void acceptsBareArraysAndSingleRules() {
        assertEquals(1, AutomationTransfer.parse("[{\"id\":\"a\",\"eventType\":\"WORLD_JOIN\"}]", Set.of()).rules().size());
        assertEquals(1, AutomationTransfer.parse("{\"id\":\"b\",\"eventType\":\"WORLD_JOIN\"}", Set.of()).rules().size());
    }

    @Test
    void rejectsJunk() {
        assertThrows(IllegalArgumentException.class, () -> AutomationTransfer.parse("", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> AutomationTransfer.parse("hello", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> AutomationTransfer.parse("{\"x\":1}", Set.of()));
    }
}
