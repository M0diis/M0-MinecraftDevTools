package me.m0dii.modules.automation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomationEventTest {
    @Test
    void exposesMetadataAttributesAlongsideEventPayload() {
        AutomationEvent event = new AutomationEvent(
                AutomationEventType.SCREEN_CHANGED,
                1_234L,
                56L,
                Map.of("toScreen", "InventoryScreen")
        );

        assertEquals("SCREEN_CHANGED", event.attribute("type"));
        assertEquals("SCREEN_CHANGED", event.attribute("eventType"));
        assertEquals(1_234L, event.attribute("timestampMs"));
        assertEquals(56L, event.attribute("clientTick"));
        assertEquals("InventoryScreen", event.attribute("toScreen"));
    }
}
