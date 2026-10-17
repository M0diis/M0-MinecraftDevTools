package me.m0dii.modules.packets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketLogTest {
    @Test
    void recordsOnlyWhileRecordingAndKeepsNewestFirst() {
        PacketLog log = new PacketLog(10);
        log.add(PacketLog.Direction.IN, "A", "", 1);
        log.setRecording(true);
        log.add(PacketLog.Direction.IN, "B", "", 2);
        log.add(PacketLog.Direction.OUT, "C", "", 3);
        List<PacketLog.Entry> all = log.snapshot("", true, true, false);
        assertEquals(List.of("C", "B"), all.stream().map(PacketLog.Entry::name).toList());
    }

    @Test
    void capacityDropsOldest() {
        PacketLog log = new PacketLog(10);
        log.setRecording(true);
        for (int i = 0; i < 25; i++) {
            log.add(PacketLog.Direction.IN, "P" + i, "", i);
        }
        List<PacketLog.Entry> all = log.snapshot("", true, true, false);
        assertEquals(10, all.size());
        assertEquals("P24", all.getFirst().name());
    }

    @Test
    void filtersByWordsDirectionAndNoise() {
        PacketLog log = new PacketLog(50);
        log.setRecording(true);
        log.add(PacketLog.Direction.IN, "ClientboundContainerSetContentPacket", "containerId=3", 1);
        log.add(PacketLog.Direction.OUT, "ServerboundContainerClickPacket", "containerId=3 slot=5", 2);
        log.add(PacketLog.Direction.IN, "ClientboundKeepAlivePacket", "id=1", 3);
        assertEquals(2, log.snapshot("container", true, true, true).size());
        assertEquals(1, log.snapshot("container -click", true, true, true).size());
        assertEquals(1, log.snapshot("container", false, true, true).size());
        assertEquals(2, log.snapshot("", true, true, true).size());
        assertEquals(3, log.snapshot("", true, true, false).size());
    }

    @Test
    void namesNestedClassesReadably() {
        assertEquals("Map.Entry", PacketLog.nameOf(java.util.Map.Entry.class));
        assertTrue(PacketLog.describe("x".repeat(50), 10).endsWith("…"));
    }
}
