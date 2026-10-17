package me.m0dii.modules.redstonetimeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedstoneTimelineTest {
    @Test
    void storesOnlyChanges() {
        RedstoneTimeline timeline = new RedstoneTimeline(10);
        assertTrue(timeline.record(1L, "lamp", 100, 0));
        assertFalse(timeline.record(1L, "lamp", 101, 0));
        assertTrue(timeline.record(1L, "lamp", 104, 15));
        assertFalse(timeline.record(1L, "lamp", 105, 15));
        assertTrue(timeline.record(1L, "lamp", 108, 0));
        RedstoneTimeline.Track track = timeline.tracks().get(0);
        assertEquals(3, track.steps().size());
        assertEquals(2, track.changes());
        assertEquals(100, timeline.firstTick());
        assertEquals(108, timeline.lastTick());
    }

    @Test
    void valueAndSegmentLookup() {
        RedstoneTimeline timeline = new RedstoneTimeline(10);
        timeline.record(1L, "wire", 10, 0);
        timeline.record(1L, "wire", 14, 7);
        timeline.record(1L, "wire", 20, 0);
        RedstoneTimeline.Track track = timeline.tracks().get(0);
        assertEquals(-1, track.valueAt(9));
        assertNull(track.segmentAt(9));
        assertEquals(0, track.valueAt(13));
        assertEquals(7, track.valueAt(14));
        assertEquals(7, track.valueAt(19));
        assertEquals(0, track.valueAt(500));
        RedstoneTimeline.Segment on = track.segmentAt(16);
        assertEquals(14, on.startTick());
        assertEquals(20, on.endTick());
        assertEquals(6, on.length(100));
        RedstoneTimeline.Segment last = track.segmentAt(25);
        assertEquals(Long.MAX_VALUE, last.endTick());
        assertEquals(11, last.length(30));
    }

    @Test
    void changesWithinOneTickKeepFinalValue() {
        RedstoneTimeline timeline = new RedstoneTimeline(10);
        timeline.record(1L, "x", 1, 0);
        timeline.record(1L, "x", 2, 15);
        timeline.record(1L, "x", 2, 0); // back to the previous value in the same tick: no change at all
        RedstoneTimeline.Track track = timeline.tracks().get(0);
        assertEquals(1, track.steps().size());
        assertEquals(0, track.changes());
        timeline.record(1L, "x", 3, 15);
        timeline.record(1L, "x", 3, 8);
        assertEquals(8, track.valueAt(3));
        assertEquals(2, track.steps().size());
    }

    @Test
    void trackLimitAndActiveFilter() {
        RedstoneTimeline timeline = new RedstoneTimeline(2);
        assertTrue(timeline.record(1L, "a", 1, 0));
        assertTrue(timeline.record(2L, "b", 1, 0));
        assertFalse(timeline.record(3L, "c", 1, 0));
        assertTrue(timeline.isFull());
        timeline.record(2L, "b", 5, 15);
        assertEquals(1, timeline.activeTracks().size());
        assertEquals("b", timeline.activeTracks().get(0).label());
        timeline.clear();
        assertTrue(timeline.isEmpty());
    }

    @Test
    void describesRedstoneTicks() {
        assertEquals("4 gt (2 rt)", RedstoneTimeline.describeTicks(4));
        assertEquals("3 gt (1.5 rt)", RedstoneTimeline.describeTicks(3));
    }
}
