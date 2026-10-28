package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import static com.rox.roundabout.Pixels.*;
import static org.junit.jupiter.api.Assertions.*;

class UiPixelsTest {
    private static final Box BUTTON = new Box(0, 0, 100, 40);

    @Test
    void aButtonShowsItsState() {
        try (UiHarness h = new UiHarness(200, 100)) {
            Theme t = h.ui.theme;
            h.ui(ui -> ui.button("b", BUTTON, "Go"));

            h.hover(150, 80);
            assertColour(t.panel, h.pixel(10, 5), "normal");
            h.hover(50, 20);
            assertColour(t.panelRaised, h.pixel(10, 5), "hovered");
            h.input.press();
            h.frame();
            assertColour(t.border, h.pixel(10, 5), "held");
            h.input.release();
            h.frame();
        }
    }

    @Test
    void anOnButtonIsTintedWithTheAccent() {
        try (UiHarness h = new UiHarness(200, 100)) {
            Theme t = h.ui.theme;
            h.ui(ui -> ui.button("b", BUTTON, "Go", true));

            h.hover(150, 80);
            assertColour(over(t.background, Theme.withAlpha(t.accent, 0.2f)), h.pixel(10, 5), "on");
            h.hover(50, 20);
            assertColour(over(t.background, Theme.withAlpha(t.accent, 0.28f)), h.pixel(10, 5), "on, hovered");
            h.input.press();
            h.frame();
            assertColour(over(t.background, Theme.withAlpha(t.accent, 0.35f)), h.pixel(10, 5), "on, held");
        }
    }

    @Test
    void theScrollbarThumbShowsTheOffsetAndVisibleShare() {
        Box track = new Box(0, 0, 10, 100);
        float[] offset = {0};
        try (UiHarness h = new UiHarness(100, 100)) {
            Theme t = h.ui.theme;
            h.ui(ui -> ui.scrollbar("s", track, offset[0], 1000, 100));

            // a tenth visible, but the thumb is at least 24 high
            h.hover(80, 80);
            assertColour(t.textFaint, h.pixel(5, 2), "thumb at the top");
            assertColour(t.textFaint, h.pixel(5, 22), "thumb at the top");
            assertColour(t.panelRaised, h.pixel(5, 26), "track below it");
            offset[0] = 450;
            h.frame();
            assertColour(t.panelRaised, h.pixel(5, 30), "track above the thumb");
            assertColour(t.textFaint, h.pixel(5, 50), "thumb halfway: y 38-62");
            assertColour(t.panelRaised, h.pixel(5, 70), "track below the thumb");
            h.hover(5, 90);
            assertColour(t.textDim, h.pixel(5, 50), "hot while the mouse is over the track");
            assertColour(t.background, h.pixel(1, 50), "the track is inset 2px");
        }
    }

    @Test
    void aScrollbarForEverythingFillsItsTrack() {
        try (UiHarness h = new UiHarness(100, 100)) {
            Theme t = h.ui.theme;
            h.ui(ui -> ui.scrollbar("s", new Box(0, 0, 10, 100), 0, 50, 100)).hover(80, 80);
            assertColour(t.textFaint, h.pixel(5, 95), "all visible");
        }
    }

    @Test
    void tooltipsSitBelowRightOfTheMouseUnlessTheyWouldLeaveTheWindow() {
        try (UiHarness h = new UiHarness(300, 200)) {
            Theme t = h.ui.theme;
            h.ui(ui -> ui.setTooltip("tip"));

            h.hover(20, 20);
            // the box is 14 right of and 18 below the mouse, 30 high, text inset 10
            assertColour(t.panelRaised, h.pixel(20 + 17, 20 + 30), "below right");
            assertColour(t.background, h.pixel(2, 20 + 30), "not left of it (sampled clear of its 12px-blur shadow)");
            h.hover(290, 190);
            assertColour(t.panelRaised, h.pixel(290 - 13, 190 - 25), "flipped above left near the corner");
        }
    }

    @Test
    void panelsAndTilesFillWithTheirColours() {
        try (UiHarness h = new UiHarness(300, 200)) {
            Theme t = h.ui.theme;
            h.ui(ui -> {
                Box content = ui.panel(new Box(0, 0, 200, 100), "Title");
                ui.readout(new Box(content.x, content.y, 80, 40), "PC", "8000", t.accent);
                ui.chip(new Box(210, 10, 40, 20), "Z", true);
                ui.chip(new Box(210, 40, 40, 20), "C", false);
                ui.fill(new Box(260, 150, 10, 10), 0xFF112233);
                ui.line(200, 190.5f, 300, 190.5f, 0xFF445566); // on the pixel centre, so not antialiased
            }).frame();

            assertColour(t.panel, h.pixel(100, 80), "panel body");
            assertNotColour(t.panel, h.pixel(100, 28), "header divider");
            assertColour(t.border, h.pixel(0, 50), "outline");
            assertColour(t.panelRaised, h.pixel(10, 38), "readout tile, inset 8 below the header");
            assertTrue(h.contains(new Box(8, 36, 80, 40), t.accent), "readout value in its colour");
            assertColour(over(t.background, Theme.withAlpha(t.good, 0.18f)), h.pixel(212, 12), "set flag");
            assertColour(t.panelRaised, h.pixel(212, 42), "clear flag");
            assertColour(0xFF112233, h.pixel(265, 155), "fill");
            assertColour(0xFF445566, h.pixel(250, 190), "line");
        }
    }
}
