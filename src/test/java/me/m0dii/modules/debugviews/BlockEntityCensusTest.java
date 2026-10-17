package me.m0dii.modules.debugviews;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockEntityCensusTest {
    @Test
    void countsByTypeAndFindsBusiestChunk() {
        BlockEntityCensus census = new BlockEntityCensus();
        assertEquals("", census.busiestChunk());
        census.add("minecraft:hopper", 3, -2);
        census.add("minecraft:hopper", 3, -2);
        census.add("minecraft:chest", 3, -2);
        census.add("minecraft:chest", -1, 0);
        census.add("minecraft:sign", -1, 0);
        assertEquals(5, census.total());
        List<BlockEntityCensus.Row> top = census.top(2);
        assertEquals(2, top.size());
        assertEquals(new BlockEntityCensus.Row("minecraft:chest", 2), top.get(0));
        assertEquals(new BlockEntityCensus.Row("minecraft:hopper", 2), top.get(1));
        assertEquals("chunk 3, -2 (3) · blocks 48..63, -32..-17", census.busiestChunk());
    }
}
