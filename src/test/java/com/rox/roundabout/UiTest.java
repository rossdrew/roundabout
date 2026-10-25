package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiTest {
    @Test
    void headingsUpperCaseTheNameButNotTheDetails() {
        assertEquals("CPU", Ui.heading("CPU"));
        assertEquals("WATCHES", Ui.heading("Watches"));
        assertEquals("MEMORY  ·  0x0000 – 0xFFFF", Ui.heading("Memory  ·  0x0000 – 0xFFFF"));
        assertEquals("FRAME TIMING  ·  ms between frames  ·  budget 16.7 ms",
                Ui.heading("Frame timing  ·  ms between frames  ·  budget 16.7 ms"));
    }
}
