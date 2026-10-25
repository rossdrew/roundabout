package com.rox.roundabout.debugger;

import com.rox.roundabout.debugger.DebugTarget.Step;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class MemoryDebuggerTest {
    @Test
    void keyHintsListEachStepWithAShortcut() {
        String hints = MemoryDebugger.keyHints(List.of(new Step("Step frame", GLFW_KEY_F, () -> {}),
                new Step("Step scanline", () -> {}), new Step("Step instr", GLFW_KEY_F7, () -> {})));

        assertTrue(hints.startsWith("Space run/pause   F step frame   F7 step instr   ↑↓"), hints);
        assertFalse(hints.contains("scanline"), "steps without a key have no hint");
    }
}
