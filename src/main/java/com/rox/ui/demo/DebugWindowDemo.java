package com.rox.ui.demo;

import com.rox.ui.debugger.MemoryDebugger;

/** Standalone memory debugger preview, driven by simulated NES activity ({@link FakeNes}). */
public final class DebugWindowDemo {
    private DebugWindowDemo() {
    }

    public static void main(String[] args) {
        FakeNes nes = new FakeNes();
        nes.start();
        new MemoryDebugger(nes, "roxUI · Memory").run();
    }
}
