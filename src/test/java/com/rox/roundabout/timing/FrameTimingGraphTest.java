package com.rox.roundabout.timing;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.UiHarness;
import org.junit.jupiter.api.Test;

import static com.rox.roundabout.Pixels.*;
import static org.junit.jupiter.api.Assertions.*;

class FrameTimingGraphTest {
    private static final long MS = 1_000_000L;
    // 140px of numbers and an 8px gap, then the graph: its plot is x 182-594, y 6-194, 0-50 ms
    private static final Box AREA = new Box(0, 0, 600, 200);

    private static FrameTimes times(float budgetMs, long... intervalsMs) {
        FrameTimes times = new FrameTimes("emu", 100, budgetMs);
        long t = 0;
        times.record(t);
        for (long ms : intervalsMs) {
            times.record(t += ms * MS);
        }
        times.update();
        return times;
    }

    private static int yOf(float ms) {
        return Math.round(194 - ms / FrameTimingGraph.RANGE_MS * 188);
    }

    @Test
    void guidesMarkTheBudgetAndLate() {
        try (UiHarness h = new UiHarness(600, 200)) {
            Theme t = h.ui.theme;
            h.ui(ui -> FrameTimingGraph.draw(ui, AREA, times(20, 16, 16, 16))).frame();

            assertEquals(yOf(20), h.rowNearest(300, 0, 200, over(t.background, Theme.withAlpha(t.good, 0.5f))), 1, "budget");
            assertEquals(yOf(30), h.rowNearest(300, 0, 200, over(t.background, Theme.withAlpha(t.hot, 0.5f))), 1, "late, 1.5x");
            // three of 100 samples: the line is only the last 8px, right-aligned
            assertEquals(yOf(16), h.rowNearest(590, 0, 200, t.accent), 1, "on-time frames in the accent");
        }
    }

    @Test
    void lateFramesTurnTheLineAndCountWarm() {
        try (UiHarness h = new UiHarness(600, 200)) {
            Theme t = h.ui.theme;
            h.ui(ui -> FrameTimingGraph.draw(ui, AREA, times(20, 16, 40, 16))).frame();

            assertTrue(h.contains(new Box(182, 0, 412, 200), t.warn), "line in warn");
            assertFalse(h.contains(new Box(182, 0, 412, 200), t.accent), "not the accent");
            assertTrue(h.contains(new Box(0, 92, 140, 18), t.hot), "late count in hot");
            assertTrue(h.contains(new Box(0, 74, 140, 18), t.hot), "max over late in hot");
        }
    }

    @Test
    void numbersAreColouredByHowLateTheyAre() {
        try (UiHarness h = new UiHarness(600, 200)) {
            Theme t = h.ui.theme;
            // rows: name 0-20, budget 20-38, mean 38-56, p99 56-74, max 74-92, late 92-110
            h.ui(ui -> FrameTimingGraph.draw(ui, AREA, times(20, 20, 20, 23))).frame();

            assertTrue(h.contains(new Box(60, 38, 80, 18), t.good), "mean 21: within 10%");
            assertTrue(h.contains(new Box(60, 74, 80, 18), t.warn), "max 23: over 10%");
            assertTrue(h.contains(new Box(60, 92, 80, 18), t.good), "none late");
        }
    }
}
