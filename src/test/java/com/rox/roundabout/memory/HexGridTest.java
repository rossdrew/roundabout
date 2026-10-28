package com.rox.roundabout.memory;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.UiHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rox.roundabout.Pixels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class HexGridTest {
    // HexGrid's layout for a 4 KB memory ("0x0FFF" gutter) drawn at 0,0: 8px scrollbar on the right,
    // 24px column header, then 10 rows of 20px; hex cells 26px wide after a 78px gutter, 8px between
    // the two groups of 8; ASCII 9px wide, 18px after the hex
    private static final Box AREA = new Box(0, 0, 800, 24 + 10 * 20);

    private static float hexX(int col) {
        return 78 + col * 26 + 13 + (col >= 8 ? 8 : 0);
    }

    private static float asciiX(int col) {
        return 78 + 16 * 26 + 8 + 18 + col * 9 + 4;
    }

    private static float rowY(int visibleRow) {
        return 24 + visibleRow * 20 + 10;
    }

    private final ArrayMemory memory = new ArrayMemory(0x1000);
    private final MemoryHeat heat = new MemoryHeat(memory, 0.35f);
    private final HexGrid grid = new HexGrid(0x1000, List.of(new MemoryRegion("All", 0, 0xFFF, 0xFF5AA9FF)),
            AddressFormat.forSize(0x1000, AddressFormat.Style.C));
    private UiHarness h;

    @BeforeEach
    void draw() {
        h = new UiHarness(800, 300);
        // dt of 1 s finishes each scroll animation within the frame
        h.ui(ui -> grid.draw(ui, AREA.copy(), heat, 1f)).frame();
    }

    @AfterEach
    void close() {
        h.close();
    }

    @Test
    void aClickSelectsTheByteUnderTheMouse() {
        h.click(hexX(5), rowY(2));
        assertEquals(0x25, grid.selected());
        h.click(hexX(9), rowY(1));
        assertEquals(0x19, grid.selected(), "second group");
        h.click(asciiX(3), rowY(4));
        assertEquals(0x43, grid.selected(), "ASCII column");
        h.click(30, rowY(4));
        assertEquals(0x43, grid.selected(), "the gutter selects nothing");
    }

    @Test
    void hoveringReportsTheAddressUnderTheMouse() {
        h.hover(hexX(15), rowY(9));
        assertEquals(0x9F, grid.hovered());
        h.hover(hexX(0), 10);
        assertEquals(-1, grid.hovered(), "header");
        h.hover(30, rowY(1));
        assertEquals(-1, grid.hovered(), "gutter");
        h.hover(hexX(7) + 12, rowY(1));
        assertEquals(0x17, grid.hovered());
        h.hover(hexX(7) + 15, rowY(1));
        assertEquals(-1, grid.hovered(), "gap between the groups");
    }

    @Test
    void aDoubleClickOrEnterActivatesTheSelection() {
        h.click(hexX(2), rowY(3));
        h.input.press();
        h.frame();
        assertEquals(0x32, grid.activated(), "on the second press");
        h.input.release();
        h.frame();
        assertEquals(-1, grid.activated(), "for one frame");

        h.click(hexX(4), rowY(0));
        assertEquals(-1, grid.activated(), "a single click only selects");
        h.key(GLFW_KEY_ENTER);
        assertEquals(0x04, grid.activated());
    }

    @Test
    void arrowsMoveTheSelectionAndStopAtTheEnds() {
        h.key(GLFW_KEY_LEFT);
        assertEquals(0, grid.selected());
        h.key(GLFW_KEY_UP);
        assertEquals(0, grid.selected());
        h.key(GLFW_KEY_RIGHT);
        assertEquals(1, grid.selected());
        h.key(GLFW_KEY_DOWN);
        assertEquals(0x11, grid.selected());
        h.key(GLFW_KEY_UP);
        assertEquals(1, grid.selected());
        h.key(GLFW_KEY_LEFT);
        assertEquals(0, grid.selected());

        h.key(GLFW_KEY_END);
        assertEquals(0xFFF, grid.selected());
        h.key(GLFW_KEY_RIGHT);
        h.key(GLFW_KEY_DOWN);
        assertEquals(0xFFF, grid.selected());
        h.key(GLFW_KEY_HOME);
        assertEquals(0, grid.selected());
    }

    @Test
    void pagesMoveByTheVisibleRows() {
        h.key(GLFW_KEY_PAGE_DOWN);
        assertEquals(10 * 16, grid.selected());
        h.key(GLFW_KEY_PAGE_DOWN);
        h.key(GLFW_KEY_PAGE_UP);
        assertEquals(10 * 16, grid.selected());
    }

    @Test
    void theViewFollowsTheSelection() {
        assertEquals(0, grid.firstVisibleAddress());
        assertEquals(0x9F, grid.lastVisibleAddress());
        for (int i = 0; i < 9; i++) {
            h.key(GLFW_KEY_DOWN);
        }
        assertEquals(0, grid.firstVisibleAddress(), "row 9 is the last visible");
        h.key(GLFW_KEY_DOWN);
        assertEquals(0x10, grid.firstVisibleAddress());
        assertEquals(0xAF, grid.lastVisibleAddress());

        h.key(GLFW_KEY_END);
        assertEquals(0xF60, grid.firstVisibleAddress());
        assertEquals(0xFFF, grid.lastVisibleAddress());
        for (int i = 0; i < 10; i++) {
            h.key(GLFW_KEY_UP);
        }
        assertEquals(0xF50, grid.firstVisibleAddress());
    }

    @Test
    void scrollToCentresTheAddressWithinRange() {
        grid.scrollTo(0x800);
        h.frame();
        assertEquals(0x800 - 5 * 16, grid.firstVisibleAddress());
        grid.scrollTo(0x10);
        h.frame();
        assertEquals(0, grid.firstVisibleAddress());
        grid.scrollTo(0xFF0);
        h.frame();
        assertEquals(0xF60, grid.firstVisibleAddress());
    }

    @Test
    void scrollingEasesTowardsItsTarget() {
        grid.scrollTo(0x800);
        h.ui(ui -> grid.draw(ui, AREA.copy(), heat, 1 / 60f)).frame();
        int first = grid.firstVisibleAddress();
        assertTrue(first > 0 && first < 0x800 - 5 * 16, "part of the way: " + first);
        for (int i = 0; i < 60; i++) {
            h.frame();
        }
        assertEquals(0x800 - 5 * 16, grid.firstVisibleAddress());
    }

    @Test
    void theWheelScrollsThreeRowsAtATime() {
        h.scroll(hexX(0), rowY(5), -1);
        assertEquals(3 * 16, grid.firstVisibleAddress());
        h.scroll(hexX(0), rowY(5), 2);
        assertEquals(0, grid.firstVisibleAddress(), "clamped at the top");
        h.scroll(hexX(0), 10, -1);
        assertEquals(0, grid.firstVisibleAddress(), "not over the header");
    }

    @Test
    void draggingTheScrollbarJumpsThere() {
        h.input.move(796, 112).press();
        h.frame();
        assertEquals((256 / 2 - 5) * 16, grid.firstVisibleAddress());
        h.input.release();
        h.frame();
    }

    @Test
    void preferredWidthFitsARowAndItsAscii() {
        assertEquals(78 + 16 * 26 + 8 + 18 + 16 * 9 + 8 + 100, grid.preferredWidth());
    }

    @Test
    void aMemorySmallerThanTheViewDoesntScroll() {
        HexGrid small = new HexGrid(64, List.of(), AddressFormat.forSize(64, AddressFormat.Style.C));
        h.ui(ui -> small.draw(ui, AREA.copy(), new MemoryHeat(new ArrayMemory(64), 1), 1f)).frame();
        small.scrollTo(48);
        h.key(GLFW_KEY_END);
        assertEquals(0, small.firstVisibleAddress());
        assertEquals(63, small.lastVisibleAddress());
        assertEquals(63, small.selected());
    }

    @Test
    void columnsStartAndEndExactlyOnTheirEdges() {
        // hex group 1: x 78-286, gap to 294, group 2: 294-502, gap to 520, ASCII: 520-664
        float[][] edges = {{77.9f, -1}, {78, 0}, {103.9f, 0}, {104, 1}, {285.9f, 7}, {286, -1}, {293.9f, -1},
                {294, 8}, {501.9f, 15}, {502, -1}, {519.9f, -1}, {520, 0}, {663.9f, 15}, {664, -1}};
        for (float[] edge : edges) {
            h.hover(edge[0], rowY(0));
            assertEquals((int) edge[1], grid.hovered(), "x " + edge[0]);
        }
    }

    @Test
    void theSpaceBelowASmallMemoryIsntBytes() {
        HexGrid small = new HexGrid(64, List.of(), AddressFormat.forSize(64, AddressFormat.Style.C));
        h.ui(ui -> small.draw(ui, AREA.copy(), new MemoryHeat(new ArrayMemory(64), 1), 1f)).frame();
        // 64 bytes is 4 rows; the gutter is 4 digits narrower ("0x3F")
        float x = 60 + 13;
        h.click(x, rowY(3));
        assertEquals(0x30, small.selected());
        h.click(x, rowY(4));
        assertEquals(0x30, small.selected(), "below the last row");
        h.hover(x, rowY(4));
        assertEquals(-1, small.hovered());
    }

    // ---- what it draws ----

    private static int rowTop(int visibleRow) {
        return 24 + visibleRow * 20;
    }

    @Test
    void aWrittenByteGlowsAndCools() {
        Theme t = h.ui.theme;
        memory.bytes[0x13] = 0x42;
        heat.update(0);
        h.frame();
        assertColour(over(t.background, t.heat(1)), h.pixel((int) hexX(3), rowTop(1) + 3), "just written");
        assertColour(over(t.background, Theme.withAlpha(t.warn, 0.15f)), h.pixel(548, rowTop(1) + 3), "ASCII too");
        assertColour(t.background, h.pixel((int) hexX(4), rowTop(1) + 3), "its neighbour");

        heat.update(0.35f);
        h.frame();
        assertColour(over(t.background, t.heat(0.5f)), h.pixel((int) hexX(3), rowTop(1) + 3), "a half-life later");
        heat.update(10);
        h.frame();
        assertColour(t.background, h.pixel((int) hexX(3), rowTop(1) + 3), "cold");
    }

    @Test
    void theSelectedByteIsOutlinedAndItsRowTinted() {
        Theme t = h.ui.theme;
        int rowTint = over(t.background, Theme.withAlpha(t.text, 0.035f));
        assertColour(t.accent, h.pixel(78, rowTop(0) + 10), "outline, left edge");
        assertColour(t.accent, h.pixel(78 + 25, rowTop(0) + 10), "outline, right edge");
        assertNotColour(t.accent, h.pixel(78 + 27, rowTop(0) + 10), "next byte");
        assertColour(over(rowTint, Theme.withAlpha(t.accent, 0.35f)), h.pixel(521, rowTop(0) + 3), "ASCII");
        assertColour(rowTint, h.pixel(700, rowTop(0) + 10), "selected row");
        assertColour(t.background, h.pixel(700, rowTop(1) + 10), "other rows");
    }

    @Test
    void theHoveredByteIsLighter() {
        Theme t = h.ui.theme;
        h.hover(hexX(5), rowY(2));
        int rowTint = over(t.background, Theme.withAlpha(t.text, 0.02f));
        assertColour(over(rowTint, Theme.withAlpha(t.text, 0.12f)), h.pixel((int) hexX(5), rowTop(2) + 3), "hovered");
        assertColour(rowTint, h.pixel((int) hexX(6), rowTop(2) + 3), "its row");
    }

    @Test
    void eachRowIsMarkedWithItsRegionsColour() {
        Theme t = h.ui.theme;
        assertColour(over(t.background, Theme.withAlpha(0xFF5AA9FF, 0.7f)), h.pixel(1, rowTop(1) + 10), "region bar");
        assertColour(t.background, h.pixel(5, rowTop(1) + 10), "3px wide");
    }

    @Test
    void scrolledRowsShowTheirOwnBytes() {
        Theme t = h.ui.theme;
        grid.scrollTo(0x800);
        memory.bytes[0x815] = 1;
        heat.update(0);
        h.frame();
        // first visible row 0x7B0, so 0x815 is on visible row 6
        assertColour(over(t.background, t.heat(1)), h.pixel((int) hexX(5), rowTop(6) + 3), "hot");
        assertColour(t.background, h.pixel((int) hexX(5), rowTop(5) + 3), "the row above");
    }
}
