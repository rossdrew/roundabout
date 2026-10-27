package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import static com.rox.roundabout.Pixels.*;
import static org.junit.jupiter.api.Assertions.*;

class DockTest {
    private static final Box AREA = new Box(0, 0, 500, 300);

    private static Box[] cutOnce(Dock.Side side) {
        Box area = AREA.copy();
        Box[] result = new Box[2];
        try (UiHarness h = new UiHarness(500, 300)) {
            h.ui(ui -> {
                result[0] = new Dock("Panel", side).cut(ui, area, 100);
                result[1] = area;
            }).frame();
        }
        return result;
    }

    @Test
    void anOpenDockCutsItsPanelAndAGutterFromItsSide() {
        Box[] left = cutOnce(Dock.Side.LEFT);
        assertEquals("Box[0.0,0.0 100.0x300.0]", left[0].toString());
        assertEquals("Box[110.0,0.0 390.0x300.0]", left[1].toString());

        Box[] right = cutOnce(Dock.Side.RIGHT);
        assertEquals("Box[400.0,0.0 100.0x300.0]", right[0].toString());
        assertEquals("Box[0.0,0.0 390.0x300.0]", right[1].toString());

        Box[] top = cutOnce(Dock.Side.TOP);
        assertEquals("Box[0.0,0.0 500.0x100.0]", top[0].toString());
        assertEquals("Box[0.0,110.0 500.0x190.0]", top[1].toString());

        Box[] bottom = cutOnce(Dock.Side.BOTTOM);
        assertEquals("Box[0.0,200.0 500.0x100.0]", bottom[0].toString());
        assertEquals("Box[0.0,0.0 500.0x190.0]", bottom[1].toString());
    }

    @Test
    void theLinkFoldsTheDockAndTheStripUnfoldsIt() {
        Dock dock = new Dock("Map", Dock.Side.LEFT);
        Box[] panel = {null};
        Box[] rest = {null};
        try (UiHarness h = new UiHarness(500, 300)) {
            h.ui(ui -> {
                rest[0] = AREA.copy();
                panel[0] = dock.cut(ui, rest[0], 100);
            });

            h.click(105, 14).frame(); // the link, in the gutter level with the panel header
            assertFalse(dock.isOpen());
            assertNull(panel[0]);
            assertEquals(Dock.FOLDED + Dock.GUTTER, rest[0].x, "folded to a strip");

            h.click(12, 150).frame();
            assertTrue(dock.isOpen());
            assertNotNull(panel[0]);
        }
    }

    @Test
    void aHorizontalDocksLinkSitsInFromTheCorner() {
        Dock dock = new Dock("Watches", Dock.Side.BOTTOM);
        try (UiHarness h = new UiHarness(500, 300)) {
            h.ui(ui -> dock.cut(ui, AREA.copy(), 100));

            h.click(24, 195); // gutter is y 190-200; link 14 + 10 in from the left
            assertFalse(dock.isOpen());
            h.click(24, 271); // folded: the strip is y 276-300, its gutter 266-276
            assertTrue(dock.isOpen());
        }
    }

    @Test
    void openStateCanBeSetDirectly() {
        Dock dock = new Dock("Map", Dock.Side.LEFT);
        assertEquals("Map", dock.title());
        dock.setOpen(false);
        assertFalse(dock.isOpen());
        dock.toggle();
        assertTrue(dock.isOpen());
    }

    @Test
    void aFoldedDockIsAStripOfItsOwn() {
        Dock dock = new Dock("Map", Dock.Side.LEFT);
        dock.setOpen(false);
        try (UiHarness h = new UiHarness(500, 300)) {
            Theme t = h.ui.theme;
            h.ui(ui -> dock.cut(ui, AREA.copy(), 100)).hover(400, 200);

            assertColour(t.panel, h.pixel(12, 200), "strip");
            assertColour(t.panel, h.pixel(22, 200), "24px wide");
            assertColour(t.background, h.pixel(26, 200), "gutter");
            h.hover(12, 200);
            assertColour(t.panelRaised, h.pixel(12, 250), "hovered");
        }
    }

    @Test
    void theLinkIsBrightWhenOpenAndDimWhenFolded() {
        Dock dock = new Dock("Map", Dock.Side.LEFT);
        Box link = new Box(94, 6, 22, 16); // across the gutter at x 100-110, level with the header
        try (UiHarness h = new UiHarness(500, 300)) {
            Theme t = h.ui.theme;
            h.ui(ui -> dock.cut(ui, AREA.copy(), 100)).hover(400, 200);
            assertTrue(h.contains(link, over(t.background, Theme.withAlpha(t.accent, 0.55f))), "open");
            assertFalse(h.contains(link, t.textDim), "open");

            dock.setOpen(false);
            h.frame();
            assertTrue(h.contains(new Box(18, 6, 22, 16), t.textDim), "folded");

            dock.setOpen(true);
            h.hover(105, 14);
            assertTrue(h.contains(link, t.accent), "hovered");
        }
    }
}
