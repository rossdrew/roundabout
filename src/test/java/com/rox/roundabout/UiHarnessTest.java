package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiHarnessTest {
    private final Box button = new Box(0, 0, 100, 40);

    @Test
    void aClickPressesAndReleasesOverTwoFrames() {
        int[] clicks = {0};
        try (UiHarness h = new UiHarness(200, 100)) {
            h.ui(ui -> {
                if (ui.button("b", button, "Go")) {
                    clicks[0]++;
                }
            });

            h.click(50, 20);
            assertEquals(1, clicks[0]);
            h.frame().frame();
            assertEquals(1, clicks[0], "a click is one frame's result");
            h.click(150, 20);
            assertEquals(1, clicks[0], "outside the button");
        }
    }

    @Test
    void twoQuickClicksAreADoubleClick() {
        boolean[] doubled = {false};
        try (UiHarness h = new UiHarness(200, 100)) {
            h.ui(ui -> doubled[0] |= ui.input.doubleClicked);

            h.click(10, 10);
            assertFalse(doubled[0]);
            h.click(10, 10);
            assertTrue(doubled[0]);
        }
    }

    @Test
    void pixelsShowWhatWasDrawn() {
        try (UiHarness h = new UiHarness(200, 100)) {
            Theme t = h.ui.theme;
            h.ui(ui -> ui.button("b", button, "Go"));

            h.hover(150, 80);
            assertEquals(t.panel, h.pixel(10, 5));
            assertEquals(t.background, h.pixel(150, 80));
            h.hover(50, 20);
            assertEquals(t.panelRaised, h.pixel(10, 5), "hovered");
        }
    }
}
