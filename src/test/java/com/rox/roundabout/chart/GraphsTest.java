package com.rox.roundabout.chart;

import com.rox.roundabout.Box;
import com.rox.roundabout.Pixels;
import com.rox.roundabout.UiHarness;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphsTest {
    private static final int COLOUR = 0xFFFF00FF;

    private static RingSeries series(int capacity, float... values) {
        RingSeries s = new RingSeries(capacity);
        for (float v : values) {
            s.add(v);
        }
        return s;
    }

    @Test
    void aSparklineIsScaledIntoItsBox() {
        try (UiHarness h = new UiHarness(220, 120)) {
            Box box = new Box(10, 10, 200, 100);
            RingSeries[] s = {series(3, 5, 5, 5)};
            h.ui(ui -> Graphs.sparkline(ui, box, s[0], 0, 10, COLOUR)).frame();
            assertEquals(60, h.rowNearest(110, 0, 120, COLOUR), 1, "halfway up");

            s[0] = series(3, 2, 2, 2);
            h.frame();
            assertEquals(90, h.rowNearest(110, 0, 120, COLOUR), 1, "a fifth up");

            s[0] = series(3, 4, 6, 20);
            h.frame();
            assertEquals(10, h.rowNearest(209, 0, 120, COLOUR), 1, "clamped to the top");
            assertEquals(60, h.rowNearest(60, 0, 120, COLOUR), 2, "rising from 4 to 6");
        }
    }

    @Test
    void aPartlyFilledSparklineIsRightAligned() {
        try (UiHarness h = new UiHarness(220, 120)) {
            Box box = new Box(10, 10, 200, 100);
            h.ui(ui -> Graphs.sparkline(ui, box, series(5, 10, 10), 0, 10, COLOUR)).frame();

            assertEquals(10, h.rowNearest(190, 0, 120, COLOUR), 1, "the last quarter");
            assertFalse(h.contains(new Box(10, 5, 140, 10), COLOUR), "nothing before it");
        }
    }

    @Test
    void oneSampleIsNotALine() {
        try (UiHarness h = new UiHarness(220, 120)) {
            h.ui(ui -> Graphs.sparkline(ui, new Box(10, 10, 200, 100), series(3, 5), 0, 10, COLOUR)).frame();
            assertFalse(h.contains(new Box(0, 0, 220, 120), COLOUR));
        }
    }

    @Test
    void aSpikeSurvivesDownsampling() {
        float[] values = new float[1000];
        values[505] = 10; // 10 samples per pixel: only the max of 500-509 keeps it
        try (UiHarness h = new UiHarness(220, 120)) {
            h.ui(ui -> Graphs.sparkline(ui, new Box(10, 10, 100, 100), series(1000, values), 0, 10, COLOUR)).frame();
            int background = h.ui.theme.background;
            Pixels.assertNotColour(background, h.pixel(60, 15), "the spike reaches near the top");
            Pixels.assertColour(background, h.pixel(60, 5), "and no higher");

        }
    }

    @Test
    void aLineGraphMarksTheLatestValueAndGuidesSitAtTheirValue() {
        try (UiHarness h = new UiHarness(300, 200)) {
            Box[] plot = {null};
            h.ui(ui -> {
                plot[0] = Graphs.lineGraph(ui, new Box(0, 0, 300, 200), series(10, 2, 4, 8), 0, 10, COLOUR,
                        1, v -> "", v -> "");
                Graphs.guide(ui, plot[0], 5, 0, 10, 0xFF00FF00);
            }).frame();

            // plot area: 34px axis on the left, 6px right, 6px top and bottom
            assertEquals("Box[34.0,6.0 260.0x188.0]", plot[0].toString());
            int guideY = h.rowNearest(150, 0, 200, com.rox.roundabout.Theme.withAlpha(0xFF00FF00, 0.5f) | 0xFF000000);
            assertEquals(100, guideY, 1);
            assertTrue(h.contains(new Box(291, 6 + 188 * 0.2f - 3, 6, 6), COLOUR), "latest-value dot at 8");
        }
    }

    @Test
    void hoveringALineGraphShowsTheSample() {
        try (UiHarness h = new UiHarness(300, 200)) {
            h.ui(ui -> Graphs.lineGraph(ui, new Box(0, 0, 300, 200), series(3, 2, 4, 8), 0, 10, COLOUR,
                    0.5f, v -> "", v -> "v" + v));
            h.hover(164, 100); // the middle sample: 4, at x 164
            assertTrue(h.contains(new Box(160, 6 + 188 * 0.6f - 4, 8, 8), COLOUR), "marker on the hovered sample");
        }
    }
}
