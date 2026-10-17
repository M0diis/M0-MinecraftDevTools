package me.m0dii.modules.farmrate;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmRateTest {
    private static Map<Integer, ItemFlowTracker.Seen> snapshot(Object... idItemCount) {
        Map<Integer, ItemFlowTracker.Seen> map = new HashMap<>();
        for (int i = 0; i < idItemCount.length; i += 3) {
            map.put((Integer) idItemCount[i], new ItemFlowTracker.Seen((String) idItemCount[i + 1], (Integer) idItemCount[i + 2]));
        }
        return map;
    }

    @Test
    void newEntitiesCountOnce() {
        ItemFlowTracker flow = new ItemFlowTracker();
        assertEquals(Map.of("bone", 2L), flow.update(snapshot(1, "bone", 2)));
        assertTrue(flow.update(snapshot(1, "bone", 2)).isEmpty());
        // picked up by a hopper: nothing new
        assertTrue(flow.update(snapshot()).isEmpty());
    }

    @Test
    void mergingIsNotNewItemsButUnseenGrowthIs() {
        ItemFlowTracker flow = new ItemFlowTracker();
        flow.update(snapshot(1, "string", 1, 2, "string", 1));
        // 2 merged into 1
        assertTrue(flow.update(snapshot(1, "string", 2)).isEmpty());
        // 1 grows by 3 with nothing vanishing: a drop merged before we saw it
        assertEquals(Map.of("string", 3L), flow.update(snapshot(1, "string", 5)));
    }

    @Test
    void ratesNeedSomeTime() {
        FarmRateCounter counter = new FarmRateCounter();
        counter.start(0);
        counter.add("bone", 10, 1_000);
        FarmRateCounter.Row early = counter.rows(10_000).get(0);
        assertTrue(Double.isNaN(early.perHour()));
        assertEquals("—", FarmRateCounter.formatRate(early.perHour()));
        counter.add("arrow", 30, 60_000);
        List<FarmRateCounter.Row> rows = counter.rows(60_000);
        assertEquals("arrow", rows.get(0).item());
        assertEquals(1800.0, rows.get(0).perHour(), 1e-6);
        assertEquals(600.0, rows.get(1).perHour(), 1e-6);
        assertEquals(40, counter.total());
    }

    @Test
    void recentRateUsesLastFiveMinutes() {
        FarmRateCounter counter = new FarmRateCounter();
        counter.start(0);
        counter.add("bone", 100, 1_000);
        counter.add("bone", 10, 9 * 60_000);
        FarmRateCounter.Row row = counter.rows(10 * 60_000).get(0);
        assertEquals(660.0, row.perHour(), 1e-6);
        assertEquals(120.0, row.recentPerHour(), 1e-6);
        assertEquals("item,total,per_hour,last_5_min_per_hour\nbone,110,660,120\n", counter.csv(10 * 60_000));
    }

    @Test
    void formats() {
        assertEquals("4:07", FarmRateCounter.formatElapsed(247_000));
        assertEquals("1:05:09", FarmRateCounter.formatElapsed(3_909_000));
        assertEquals("12.3k", FarmRateCounter.formatRate(12_345));
        assertEquals("950", FarmRateCounter.formatRate(949.6));
    }
}
