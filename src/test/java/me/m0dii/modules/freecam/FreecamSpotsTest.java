package me.m0dii.modules.freecam;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FreecamSpotsTest {
    private static FreecamSpots.Spot spot(double x) {
        return new FreecamSpots.Spot("minecraft:overworld", x, 70, -5, 90f, 10f);
    }

    @Test
    void slotsPerWorld() {
        FreecamSpots spots = new FreecamSpots();
        spots.set("server:a", 3, spot(1));
        spots.set("server:a", 1, spot(2));
        spots.set("world:b", 1, spot(3));
        assertEquals(List.of(1, 3), spots.used("server:a"));
        assertEquals(2, spots.firstFree("server:a"));
        assertEquals(3.0, spots.get("world:b", 1).x(), 0.0);
        assertNull(spots.get("world:b", 2));
        assertTrue(spots.clear("server:a", 3));
        assertEquals(List.of(1), spots.used("server:a"));
        assertThrows(IllegalArgumentException.class, () -> spots.set("x", 10, spot(0)));
    }

    @Test
    void nextWrapsAround() {
        FreecamSpots spots = new FreecamSpots();
        assertEquals(-1, spots.next("w", 0));
        spots.set("w", 2, spot(0));
        spots.set("w", 5, spot(0));
        assertEquals(2, spots.next("w", 0));
        assertEquals(5, spots.next("w", 2));
        assertEquals(2, spots.next("w", 5));
    }

    @Test
    void jsonRoundTripSkipsJunk() {
        FreecamSpots spots = new FreecamSpots();
        spots.set("w", 4, spot(12.5));
        FreecamSpots back = FreecamSpots.fromJson(spots.toJson());
        assertEquals(spots.get("w", 4), back.get("w", 4));
        FreecamSpots junk = FreecamSpots.fromJson("{\"w\":{\"0\":{\"dimension\":\"d\",\"x\":1,\"y\":2,\"z\":3},\"x\":{},\"2\":{\"x\":1}}}");
        assertTrue(junk.used("w").isEmpty());
        assertTrue(FreecamSpots.fromJson("null").used("w").isEmpty());
    }

    @Test
    void describesSpot() {
        assertEquals("1.0 70.0 -5.0 (overworld)", spot(1).describe());
    }
}
