package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UiTest {
    private static final Box A = new Box(0, 0, 100, 40);
    private static final Box B = new Box(150, 0, 100, 40);

    @Test
    void headingsUpperCaseTheNameButNotTheDetails() {
        assertEquals("CPU", Ui.heading("CPU"));
        assertEquals("WATCHES", Ui.heading("Watches"));
        assertEquals("MEMORY  ·  0x0000 – 0xFFFF", Ui.heading("Memory  ·  0x0000 – 0xFFFF"));
        assertEquals("FRAME TIMING  ·  ms between frames  ·  budget 16.7 ms",
                Ui.heading("Frame timing  ·  ms between frames  ·  budget 16.7 ms"));
    }

    @Test
    void nothingElseIsHoveredWhileAWidgetHoldsTheMouse() {
        boolean[] hoverB = {false};
        try (UiHarness h = new UiHarness(300, 100)) {
            h.ui(ui -> {
                ui.drag("a", A);
                hoverB[0] = ui.hovered(B);
            });

            h.input.move(50, 20).press();
            h.frame();
            h.hover(200, 20);
            assertFalse(hoverB[0], "A still holds the mouse");
            h.input.release();
            h.frame();
            h.frame();
            assertTrue(hoverB[0]);
        }
    }

    @Test
    void aDragKeepsTheMouseWhenItLeavesTheWidget() {
        boolean[] dragging = {false, false};
        try (UiHarness h = new UiHarness(300, 100)) {
            h.ui(ui -> {
                dragging[0] = ui.drag("a", A);
                dragging[1] = ui.drag("b", B);
            });

            h.input.move(50, 20).press();
            h.frame();
            assertTrue(dragging[0]);
            h.hover(200, 20);
            assertTrue(dragging[0], "held outside A");
            assertFalse(dragging[1], "B can't claim a held mouse");
            assertTrue(h.ui.isActive("a"));
            h.input.release();
            h.frame();
            assertFalse(dragging[0]);
            h.frame();
            assertFalse(h.ui.isActive("a"));
        }
    }

    @Test
    void aClickNeedsThePressAndReleaseOnTheWidget() {
        int[] clicks = {0};
        try (UiHarness h = new UiHarness(300, 100)) {
            h.ui(ui -> {
                if (ui.clicked("a", A)) {
                    clicks[0]++;
                }
            });

            h.input.move(50, 20).press();
            h.frame();
            h.input.move(200, 20).release();
            h.frame();
            assertEquals(0, clicks[0], "released elsewhere");

            h.input.move(200, 20).press();
            h.frame();
            h.input.move(50, 20).release();
            h.frame();
            assertEquals(0, clicks[0], "pressed elsewhere");

            h.click(50, 20);
            assertEquals(1, clicks[0]);
        }
    }

    @Test
    void draggingTheScrollbarJumpsCentredOnTheMouse() {
        Box track = new Box(0, 0, 10, 100);
        float[] offset = {0};
        try (UiHarness h = new UiHarness(100, 100)) {
            h.ui(ui -> offset[0] = ui.scrollbar("s", track, offset[0], 1000, 100));

            h.frame();
            assertEquals(0, offset[0], "unchanged until dragged");
            h.input.move(5, 50).press();
            h.frame();
            assertEquals(450, offset[0]);
            h.hover(5, 0);
            assertEquals(0, offset[0]);
            h.hover(5, 99);
            assertEquals(900, offset[0], "clamped to the end");
        }
    }

    @Test
    void overlaysDrawAfterEverythingElse() {
        List<String> order = new ArrayList<>();
        try (UiHarness h = new UiHarness(100, 100)) {
            h.ui(ui -> {
                ui.overlay(() -> order.add("overlay"));
                order.add("widget");
            });

            h.frame();
            assertEquals(List.of("widget", "overlay"), order);
            h.frame();
            assertEquals(List.of("widget", "overlay", "widget", "overlay"), order, "overlays run once");
        }
    }

    @Test
    void aTooltipIsDrawnOverTheFrame() {
        try (UiHarness h = new UiHarness(300, 200)) {
            boolean[] tip = {false};
            h.ui(ui -> {
                if (tip[0]) {
                    ui.setTooltip("tip");
                }
            });
            h.hover(20, 20);
            int without = h.pixel(60, 50);
            tip[0] = true;
            h.hover(20, 20);
            assertNotEquals(without, h.pixel(60, 50), "below and right of the mouse");
        }
    }
}
