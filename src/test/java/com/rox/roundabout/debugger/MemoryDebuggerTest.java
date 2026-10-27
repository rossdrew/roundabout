package com.rox.roundabout.debugger;

import com.rox.roundabout.UiHarness;
import com.rox.roundabout.debugger.DebugTarget.Step;
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
}
