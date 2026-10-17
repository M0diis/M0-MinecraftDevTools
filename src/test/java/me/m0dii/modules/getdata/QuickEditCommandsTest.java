package me.m0dii.modules.getdata;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QuickEditCommandsTest {
    private static Map<String, Boolean> flags(String... on) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        for (String flag : QuickEditCommands.ENTITY_FLAGS) {
            map.put(flag, List.of(on).contains(flag));
        }
        return map;
    }

    @Test
    void onlyChangedFieldsAreMerged() {
        var before = new QuickEditCommands.EntityValues("Bob", "20", flags("Silent"));
        var after = new QuickEditCommands.EntityValues("Bob \"the\" Cow", "10.5", flags("Silent", "Glowing"));
        List<String> commands = QuickEditCommands.entity("entity abc", before, after);
        assertEquals(List.of("data merge entity abc {CustomName:\"Bob \\\"the\\\" Cow\",Health:10.5f,Glowing:1b}"), commands);
    }

    @Test
    void noChangesMeansNoCommands() {
        var values = new QuickEditCommands.EntityValues("", null, flags());
        assertTrue(QuickEditCommands.entity("entity abc", values, values).isEmpty());
    }

    @Test
    void clearingTheNameRemovesIt() {
        var before = new QuickEditCommands.EntityValues("Bob", "20", flags("NoAI"));
        var after = new QuickEditCommands.EntityValues(" ", "20", flags());
        assertEquals(List.of("data merge entity abc {NoAI:0b}", "data remove entity abc CustomName"),
                QuickEditCommands.entity("entity abc", before, after));
    }

    @Test
    void rejectsBadHealth() {
        var before = new QuickEditCommands.EntityValues("", "20", flags());
        assertThrows(IllegalArgumentException.class,
                () -> QuickEditCommands.entity("entity abc", before, new QuickEditCommands.EntityValues("", "lots", flags())));
        assertThrows(IllegalArgumentException.class,
                () -> QuickEditCommands.entity("entity abc", before, new QuickEditCommands.EntityValues("", "0", flags())));
        assertEquals(List.of("data merge entity abc {Health:5.0f}"),
                QuickEditCommands.entity("entity abc", before, new QuickEditCommands.EntityValues("", "5", flags())));
    }

    @Test
    void blockStateHelpers() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("facing", "north");
        props.put("half", "top");
        assertEquals("[facing=north,half=top]", QuickEditCommands.stateSuffix(props));
        assertEquals("", QuickEditCommands.stateSuffix(Map.of()));
        List<String> values = List.of("north", "east", "south", "west");
        assertEquals("east", QuickEditCommands.cycle(values, "north", 1));
        assertEquals("north", QuickEditCommands.cycle(values, "west", 1));
        assertEquals("west", QuickEditCommands.cycle(values, "north", -1));
    }
}
