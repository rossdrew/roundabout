package com.rox.ui.debugger;

import com.rox.ui.data.MemoryRegion;
import com.rox.ui.data.MemorySource;

import java.util.List;

/** What {@link MemoryDebugger} inspects and controls - implemented by an emulator adapter. */
public interface DebugTarget {

    /** 6502-style registers; {@code p} is the NV-BDIZC status byte. */
    record CpuState(int pc, int a, int x, int y, int sp, int p) {
    }

    /** A named address to watch from the start. */
    record Label(String name, int address) {
    }

    MemorySource memory();

    List<MemoryRegion> regions();

    default List<Label> labels() {
        return List.of();
    }

    CpuState cpu();

    boolean isPaused();

    void pause();

    void resume();

    void stepFrame();

    void stepInstruction();

    /** A short, human-readable run summary for the top bar, e.g. a frame count and speed. */
    String status();
}
