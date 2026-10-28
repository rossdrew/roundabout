package com.rox.roundabout.memory;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.UiHarness;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rox.roundabout.Pixels.*;
import static org.junit.jupiter.api.Assertions.*;

class MemoryMapTest {
    private static final Box AREA = new Box(0, 0, 200, 400);
    private static final AddressFormat FORMAT = AddressFormat.forSize(0x1000, AddressFormat.Style.C);
    private final MemoryHeat heat = new MemoryHeat(new ArrayMemory(0x1000), 1);

    private int[] drawn(MemoryMap map, UiHarness h) {
        int[] jump = {-1};
        h.ui(ui -> {
            int target = map.draw(ui, AREA.copy(), heat, 0, 0xFF);
            if (target >= 0) {
                jump[0] = target;
            }
        });
        return jump;
    }

    @Test
    void equalRegionsGetEqualBands() {
        MemoryMap map = new MemoryMap(List.of(new MemoryRegion("Low", 0, 0x7FF, 0), new MemoryRegion("High", 0x800, 0xFFF, 0)), FORMAT);
        try (UiHarness h = new UiHarness(200, 400)) {
            int[] jump = drawn(map, h);

            h.click(100, 100);
            assertEquals(0x400, jump[0]);
            h.click(100, 300);
            assertEquals(0xC00, jump[0]);
            h.click(100, 0);
            assertEquals(0, jump[0]);
        }
    }

    @Test
    void draggingPastTheEndStopsAtTheLastAddress() {
        MemoryMap map = new MemoryMap(List.of(new MemoryRegion("All", 0, 0xFFF, 0)), FORMAT);
        try (UiHarness h = new UiHarness(200, 600)) {
            int[] jump = drawn(map, h);

            h.input.move(100, 200).press();
            h.frame();
            assertEquals(0x800, jump[0]);
            h.hover(100, 550);
            assertEquals(0xFF5, jump[0], "still dragging, clamped to the last pixel row");
            h.input.release();
            h.frame();
            jump[0] = -1;
            h.hover(100, 100);
            assertEquals(-1, jump[0], "hovering doesn't jump");
        }
    }

    @Test
    void smallRegionsGetMoreThanTheirShare() {
        // sqrt sizing: 256 bytes of 4 KB gets a quarter of the height (plus the minimum), not 1/16
        MemoryMap map = new MemoryMap(List.of(new MemoryRegion("Zero page", 0, 0xFF, 0), new MemoryRegion("Rest", 0x100, 0xFFF, 0)), FORMAT);
        try (UiHarness h = new UiHarness(200, 400)) {
            int[] jump = drawn(map, h);

            h.click(100, 90);
            assertTrue(jump[0] >= 0xE0 && jump[0] <= 0xFF, "zero page: " + jump[0]);
            h.click(100, 100);
            assertTrue(jump[0] >= 0x100 && jump[0] < 0x150, "just into the rest: " + jump[0]);
        }
    }

    // ---- what it draws ----

    private static final int LOW = 0xFF5AA9FF, HIGH = 0xFF6FDC9C;

    @Test
    void bandsHeatAndTheViewAreDrawnInPlace() {
        ArrayMemory bytes = new ArrayMemory(0x1000);
        MemoryHeat hot = new MemoryHeat(bytes, 1);
        MemoryMap map = new MemoryMap(List.of(new MemoryRegion("Low", 0, 0x7FF, LOW), new MemoryRegion("High", 0x800, 0xFFF, HIGH)), FORMAT);
        try (UiHarness h = new UiHarness(200, 400)) {
            Theme t = h.ui.theme;
            h.ui(ui -> map.draw(ui, AREA.copy(), hot, 0x600, 0x6FF));
            bytes.bytes[0x400] = 1;
            hot.update(0);
            h.hover(-10, -10);

            int low = over(t.background, Theme.withAlpha(LOW, 0.05f));
            int high = over(t.background, Theme.withAlpha(HIGH, 0.05f));
            assertColour(low, h.pixel(100, 130), "low band");
            assertColour(high, h.pixel(100, 300), "high band");
            assertColour(over(low, Theme.withAlpha(LOW, 0.8f)), h.pixel(1, 130), "low's edge");
            assertColour(over(high, Theme.withAlpha(HIGH, 0.8f)), h.pixel(1, 300), "high's edge");

            // 0x400 is 100px down; the 0x600-0x6FF view is y 150-175
            assertColour(over(low, t.heat(1)), h.pixel(190, 100), "heat column, full width when just written");
            assertColour(over(low, t.heat(1)), h.pixel(160, 100), "heat column, full width when just written");
            assertColour(low, h.pixel(190, 120), "no heat elsewhere");
            assertColour(over(low, Theme.withAlpha(t.accent, 0.10f)), h.pixel(100, 162), "inside the view");

            hot.update(1);
            h.frame();
            assertColour(low, h.pixel(160, 100), "half heat: narrower");
            assertNotColour(low, h.pixel(190, 100), "half heat: still showing");

            h.hover(100, 300);
            assertColour(over(t.background, Theme.withAlpha(HIGH, 0.10f)), h.pixel(100, 250), "hovered band");
        }
    }
}
