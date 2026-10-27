package com.rox.roundabout.timing;

import com.rox.roundabout.chart.RingSeries;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameTimesTest {
    private static final long MS = 1_000_000L;

    @Test
    void intervalsAreTheGapsBetweenRecordedEvents() {
        FrameTimes times = new FrameTimes("emu", 10, 16);
        times.record(0);
        times.record(16 * MS);
        times.update();
        times.record(50 * MS);
        times.update();

        RingSeries intervals = times.intervalsMs();
        assertEquals(2, intervals.size());
        assertEquals(16, intervals.get(0));
        assertEquals(34, intervals.get(1));
        assertEquals("emu", times.name());
    }

    @Test
    void updatingWithNothingNewChangesNothing() {
        FrameTimes times = new FrameTimes("emu", 10, 16);
        times.record(0);
        times.update();
        times.update();
        assertEquals(0, times.intervalsMs().size());
    }

    @Test
    void eventsLostToOverflowDontMakeAFakeInterval() {
        FrameTimes times = new FrameTimes("emu", 4000, 16);
        times.record(0);
        times.update();
        for (int i = 1; i <= 3000; i++) {
            times.record(1000 * MS + i * MS);
        }
        times.update();

        RingSeries intervals = times.intervalsMs();
        assertEquals(1023, intervals.size()); // the newest 1024 events, so 1023 gaps
        assertEquals(1, intervals.max());
    }

    @Test
    void eachStreamHasItsOwnBudget() {
        assertEquals(20, new FrameTimes("pal", 10, 20).budgetMs());
        assertThrows(IllegalArgumentException.class, () -> new FrameTimes("none", 10, 0));
    }

    @Test
    void aFullBufferLosesNothing() {
        FrameTimes times = new FrameTimes("emu", 4000, 16);
        times.record(0);
        times.update();
        for (int i = 1; i <= 1024; i++) {
            times.record(i * MS);
        }
        times.update();

        assertEquals(1024, times.intervalsMs().size(), "1024 unread events fit, and join up with the last one read");
    }

    @Test
    void eventsAlreadyReadDontCountTowardsTheBuffer() {
        FrameTimes times = new FrameTimes("emu", 4000, 16);
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 600; i++) {
                times.record((round * 600 + i) * MS);
            }
            times.update();
        }

        assertEquals(1799, times.intervalsMs().size());
    }
}
