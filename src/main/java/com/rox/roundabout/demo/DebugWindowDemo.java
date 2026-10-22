package com.rox.roundabout.demo;

import com.rox.roundabout.debugger.MemoryDebugger;

/** Standalone memory debugger preview, driven by simulated NES activity ({@link FakeNes}). */
public final class DebugWindowDemo {
    private DebugWindowDemo() {
    }

    public static void main(String[] args) {
        FakeNes nes = new FakeNes();
        nes.start();
        new MemoryDebugger(nes, "Roundabout · Memory").run();
    }
}
