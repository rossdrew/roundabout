package com.rox.roundabout.memory;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.UiHarness;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.rox.roundabout.Pixels.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class MemoryPanelTest {
    // Layout in a 1200x800 area: memory map 0-200 (gutter to 210); watches docked below from y 570, the list
    // 340 wide; the hex panel's content at 218,36, so its grid body starts at y 60 and cells after a 78px gutter.
    // Watch rows are 40px from y 606.
    private static final Box AREA = new Box(0, 0, 1200, 800);
    private static final List<MemoryRegion> REGIONS = List.of(new MemoryRegion("Low", 0, 0x7FF, 0xFF5AA9FF),
            new MemoryRegion("High", 0x800, 0xFFF, 0xFF6FDC9C));

    private static float hexX(int col) {
        return 218 + 78 + col * 26 + 13 + (col >= 8 ? 8 : 0);
    }

    private static float hexY(int visibleRow) {
        return 60 + visibleRow * 20 + 10;
    }

    private static float watchY(int row) {
        return 606 + row * 40 + 19;
    }

    private final ArrayMemory memory = new ArrayMemory(0x1000);
    private final UiHarness h = new UiHarness(1200, 800);

    private MemoryPanel panel(NamedAddress... labels) {
        MemoryPanel panel = new MemoryPanel(memory, REGIONS, List.of(labels));
        h.ui(ui -> {
            panel.update(1 / 60f);
            panel.handleKeys(ui.input);
            panel.draw(ui, AREA.copy(), 1f);
        }).frame();
        return panel;
    }

    private static List<Integer> watched(MemoryPanel panel) {
        return panel.watches().stream().map(MemoryPanel.Watch::address).toList();
    }

    @AfterEach
    void close() {
        h.close();
    }

    @Test
    void labelsAreWatchedFromTheStart() {
        MemoryPanel panel = panel(new NamedAddress("score", 0x10), new NamedAddress("lives", 0x20));

        assertEquals(List.of(0x10, 0x20), watched(panel));
        assertEquals("score", panel.watches().getFirst().label());
    }

    @Test
    void wWatchesTheSelectedByteNamedForItsRegion() {
        MemoryPanel panel = panel();
        h.click(hexX(3), hexY(2)).key(GLFW_KEY_W);

        assertEquals(List.of(0x23), watched(panel));
        assertEquals("Low", panel.watches().getFirst().label());

        h.click(hexX(4), hexY(2)).key(GLFW_KEY_W);
        h.click(hexX(3), hexY(2)).key(GLFW_KEY_W);
        assertEquals(List.of(0x23, 0x24), watched(panel), "no duplicates");
        assertEquals(0, panel.selectedWatch(), "watching it again selects it");
    }

    @Test
    void doubleClickingAByteWatchesIt() {
        MemoryPanel panel = panel();
        h.doubleClick(hexX(1), hexY(1));

        assertEquals(List.of(0x11), watched(panel));
    }

    @Test
    void deleteRemovesTheSelectedWatch() {
        MemoryPanel panel = panel(new NamedAddress("a", 1), new NamedAddress("b", 2), new NamedAddress("c", 3));
        h.click(300, watchY(2));
        assertEquals(2, panel.selectedWatch());

        h.key(GLFW_KEY_DELETE);
        assertEquals(List.of(1, 2), watched(panel));
        assertEquals(1, panel.selectedWatch(), "the selection moves to the new last");
        h.key(GLFW_KEY_DELETE).key(GLFW_KEY_DELETE).key(GLFW_KEY_DELETE);
        assertEquals(List.of(), watched(panel));
    }

    @Test
    void keysCanBeRebound() {
        MemoryPanel panel = new MemoryPanel(memory, REGIONS, List.of(new NamedAddress("a", 1)));
        h.ui(ui -> {
            panel.handleKeys(ui.input, GLFW_KEY_P, GLFW_KEY_UNKNOWN);
            panel.draw(ui, AREA.copy(), 1f);
        });

        h.click(hexX(5), hexY(0)).key(GLFW_KEY_W).key(GLFW_KEY_DELETE);
        assertEquals(List.of(1), watched(panel), "W and Del do nothing");
        h.key(GLFW_KEY_P);
        assertEquals(List.of(1, 5), watched(panel));
    }

    @Test
    void clickingAWatchSelectsItAndADoubleClickShowsItsByte() {
        MemoryPanel panel = panel(new NamedAddress("a", 0x10), new NamedAddress("far", 0xF00));
        h.click(300, watchY(1));
        assertEquals(1, panel.selectedWatch());
        assertEquals(0, panel.hex().firstVisibleAddress(), "a single click doesn't move the hex view");

        h.doubleClick(300, watchY(1));
        assertEquals(0xF00 - 12 * 16, panel.hex().firstVisibleAddress());
    }

    @Test
    void theListScrollsToRevealANewWatch() {
        MemoryPanel panel = panel();
        for (int col = 0; col < 6; col++) {
            h.click(hexX(col), hexY(0)).key(GLFW_KEY_W);
        }
        assertEquals(5, panel.selectedWatch());

        // six rows (240px) in 186px: scrolled 54px so the last is in view
        h.click(300, watchY(4) - 54);
        assertEquals(4, panel.selectedWatch());

        h.scroll(300, 700, 1);
        h.click(300, watchY(3) - 14);
        assertEquals(3, panel.selectedWatch(), "wheel scrolled up a row");
    }

    @Test
    void historyIsSampledByTime() {
        MemoryPanel panel = new MemoryPanel(memory, REGIONS, List.of(new NamedAddress("a", 7)));
        memory.bytes[7] = 0x42;
        panel.update(1.01f); // a second is 59.99... samples in floats; the remainder carries over

        assertEquals(60, panel.watches().getFirst().history().size());
        assertEquals(0x42, panel.watches().getFirst().history().latest());
    }

    @Test
    void theFocusIsTheHoveredByteElseTheSelectedOne() {
        MemoryPanel panel = panel();
        h.click(hexX(2), hexY(0));
        h.hover(hexX(6), hexY(1));
        assertEquals(0x16, panel.focusAddress());
        h.hover(600, 700);
        assertEquals(0x02, panel.focusAddress());
    }

    @Test
    void clickingTheMapMovesTheHexView() {
        MemoryPanel panel = panel();
        h.click(100, 780);

        assertTrue(panel.hex().firstVisibleAddress() > 0xE00, "near the end: " + panel.hex().firstVisibleAddress());
    }

    @Test
    void accessors() {
        MemoryPanel panel = new MemoryPanel(memory, REGIONS, List.of(), AddressFormat.Style.MOTOROLA);
        assertSame(memory, panel.memory());
        assertEquals("$0FFF", panel.format().address(0xFFF));
        assertEquals(0x1000, panel.heat().size());
    }

    @Test
    void theListOnlyScrollsWithinItsLengthAndUnderTheMouse() {
        MemoryPanel panel = panel();
        for (int col = 0; col < 6; col++) {
            h.click(hexX(col), hexY(0)).key(GLFW_KEY_W);
        }
        h.scroll(300, 700, 5);
        h.scroll(600, 700, -5);
        h.click(300, watchY(0));
        assertEquals(0, panel.selectedWatch(), "scrolled to the top; the wheel elsewhere did nothing");

        h.scroll(300, 700, -5);
        h.click(300, watchY(4) - 54);
        assertEquals(4, panel.selectedWatch(), "scrolled no further than the end");
    }

    @Test
    void theSelectedWatchIsTintedAndAHoveredOneRaised() {
        panel(new NamedAddress("a", 1), new NamedAddress("b", 2), new NamedAddress("c", 3));
        Theme t = h.ui.theme;
        h.hover(600, 400);
        assertColour(Theme.mix(t.panel, t.accent, 0.12f), h.pixel(221, (int) watchY(0)), "selected");
        assertColour(t.panel, h.pixel(221, (int) watchY(1)), "not selected");
        h.hover(300, watchY(1));
        assertColour(t.panelRaised, h.pixel(221, (int) watchY(1)), "hovered");
    }
}
