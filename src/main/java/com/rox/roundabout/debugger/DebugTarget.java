package com.rox.roundabout.debugger;

import com.rox.roundabout.data.FrameTimes;
import com.rox.roundabout.data.MemoryRegion;
import com.rox.roundabout.data.MemorySource;

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

    /** Frame streams to show in a frame-timing panel, e.g. frames produced and frames shown; none by default. */
    default List<FrameTimes> frameTimings() {
        return List.of();
    }

    /** The intended interval between frames, for the frame-timing panel's budget marker. */
    default float frameBudgetMs() {
        return 1000f / 60;
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
