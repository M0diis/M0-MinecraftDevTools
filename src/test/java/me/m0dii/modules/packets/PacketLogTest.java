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

    @Test
    void trafficBucketsPerSecond() {
        PacketTraffic traffic = new PacketTraffic();
        traffic.add(100, 10_000);
        traffic.add(50, 10_900);
        traffic.add(30, 12_000);
        long[] h = traffic.history(12_500);
        assertEquals(60, h.length);
        assertEquals(30, h[59]);
        assertEquals(0, h[58]);
        assertEquals(150, h[57]);
        assertEquals(180, traffic.total());
        assertEquals(75.0, traffic.average(12_500, 2), 1e-9);
        // a minute later the old seconds are gone
        assertEquals(0, traffic.history(80_000)[59]);
        assertEquals("1.5 KB", PacketTraffic.bytes(1536));
    }

    @Test
    void sizesPinAndExport() {
        PacketLog log = new PacketLog(10);
        log.setRecording(true);
        log.pin("Chat");
        long a = log.add(PacketLog.Direction.IN, "Chat", "hi\tthere", 1_000, 120);
        long b = log.add(PacketLog.Direction.OUT, "Move", "x", 1_000, -1);
        log.add(PacketLog.Direction.IN, "Chat", "again", 1_200, -1);
        assertEquals(120, log.sizeOf(a));
        assertEquals(-1, log.sizeOf(b));
        log.setSize(b, PacketLog.Direction.OUT, "Move", 30, 1_300);
        assertEquals(30, log.sizeOf(b));
        assertEquals(2, log.pinnedRate().history(1_500)[59]);
        assertEquals(List.of("Chat", "Move"), log.knownNames());
        assertEquals("IN Chat", log.topBytes(1).getFirst().getKey());
        String tsv = log.export(new java.text.SimpleDateFormat("ss"));
        assertTrue(tsv.contains("IN\tChat\t120\thi there"));
        log.countBytes(PacketLog.Direction.IN, 500, 1_000);
        assertEquals(500, log.bytesIn().total());
        log.clear();
        assertEquals(0, log.size());
    }
}
