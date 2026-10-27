package com.rox.roundabout.debugger;

import com.rox.roundabout.UiHarness;
import com.rox.roundabout.debugger.DebugTarget.Step;
import com.rox.roundabout.timing.FrameTimes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class MemoryDebuggerTest {
    private static final List<Step> STEPS = List.of(new Step("Step frame", GLFW_KEY_F, () -> {}),
            new Step("Step scanline", () -> {}), new Step("Step instr", GLFW_KEY_F7, () -> {}));

    @Test
    void keyHintsListEachStepWithAShortcut() {
        String hints = MemoryDebugger.keyHints(Keymap.DEFAULT, STEPS);

        assertEquals("Space run/pause   F step frame   F7 step instr   ↑↓ PgUp PgDn navigate   "
                + "W / double-click watch   double-click watch to jump   Del unwatch", hints);
        assertFalse(hints.contains("scanline"), "steps without a key have no hint");
    }

    @Test
    void keyHintsFollowTheKeymap() {
        String hints = MemoryDebugger.keyHints(new Keymap(GLFW_KEY_P, GLFW_KEY_UNKNOWN, GLFW_KEY_BACKSPACE), List.of());

        assertEquals("P run/pause   ↑↓ PgUp PgDn navigate   double-click watch   double-click watch to jump   "
                + "Backspace unwatch", hints);
    }

    @Test
    void aKeymapCantBindAKeyTwiceOrTakeANavigationKey() {
        assertThrows(IllegalArgumentException.class, () -> new Keymap(GLFW_KEY_W, GLFW_KEY_W, GLFW_KEY_DELETE));
        assertThrows(IllegalArgumentException.class, () -> new Keymap(GLFW_KEY_ENTER, GLFW_KEY_W, GLFW_KEY_DELETE));
        new Keymap(GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN, GLFW_KEY_DELETE);
    }

    @Test
    void aStepCantReuseABoundKey() {
        IllegalArgumentException clash = assertThrows(IllegalArgumentException.class,
                () -> Keymap.DEFAULT.checkNoClash(List.of(new Step("Step frame", GLFW_KEY_SPACE, () -> {}))));
        assertEquals("\"Step frame\" uses Space, already bound to run/pause", clash.getMessage());
        assertThrows(IllegalArgumentException.class,
                () -> Keymap.DEFAULT.checkNoClash(List.of(new Step("Step", GLFW_KEY_DOWN, () -> {}))));

        new Keymap(GLFW_KEY_P, GLFW_KEY_W, GLFW_KEY_DELETE)
                .checkNoClash(List.of(new Step("Step frame", GLFW_KEY_SPACE, () -> {})));
    }

    @Test
    void aWholeFrameDrawsOffScreenAndTakesKeys() {
        FakeTarget target = new FakeTarget();
        MemoryDebugger debugger = new MemoryDebugger(target, "test");
        try (UiHarness h = new UiHarness(1440, 860)) {
            h.scene(debugger::frame).frame();

            h.key(GLFW_KEY_F);
            assertEquals(0, target.steps, "steps only run while paused");
            h.key(GLFW_KEY_SPACE);
            assertTrue(target.paused);
            h.key(GLFW_KEY_F);
            assertEquals(1, target.steps);
        }
    }

    // the top bar's controls: Run/Pause at x 114-206 after the title, then each step's button from x 214
    private static final float BAR_Y = 24, RUN_X = 160, STEP_X = 260;

    @Test
    void theRunButtonTogglesAndStepButtonsOnlyWorkWhilePaused() {
        FakeTarget target = new FakeTarget();
        try (UiHarness h = new UiHarness(1440, 860)) {
            h.scene(new MemoryDebugger(target, "test")::frame).frame();

            h.click(STEP_X, BAR_Y);
            assertEquals(0, target.steps);
            h.click(RUN_X, BAR_Y);
            assertTrue(target.paused);
            h.click(STEP_X, BAR_Y);
            assertEquals(1, target.steps);
            h.click(RUN_X, BAR_Y);
            assertFalse(target.paused);
        }
    }

    @Test
    void aCustomKeymapReplacesTheDefaultKeys() {
        FakeTarget target = new FakeTarget();
        MemoryDebugger debugger = new MemoryDebugger(target, "test", new Keymap(GLFW_KEY_P, GLFW_KEY_UNKNOWN, GLFW_KEY_UNKNOWN));
        try (UiHarness h = new UiHarness(1440, 860)) {
            h.scene(debugger::frame).frame();

            h.key(GLFW_KEY_SPACE);
            assertFalse(target.paused);
            h.key(GLFW_KEY_P);
            assertTrue(target.paused);
            h.key(GLFW_KEY_P);
            assertFalse(target.paused);
        }
    }

    @Test
    void aStepOnABoundKeyIsRejected() {
        FakeTarget target = new FakeTarget();
        assertThrows(IllegalArgumentException.class,
                () -> new MemoryDebugger(target, "test", new Keymap(GLFW_KEY_F, GLFW_KEY_W, GLFW_KEY_DELETE)));
    }

    @Test
    void frameTimingsAreReadEveryFrame() {
        FakeTarget target = new FakeTarget();
        FrameTimes times = new FrameTimes("emu", 100, 16);
        target.frameTimings = List.of(times, new FrameTimes("shown", 100, 16));
        try (UiHarness h = new UiHarness(1440, 860)) {
            h.scene(new MemoryDebugger(target, "test")::frame);
            times.record(0);
            times.record(16_000_000);
            h.frame();

            assertEquals(1, times.intervalsMs().size());
        }
    }
}
