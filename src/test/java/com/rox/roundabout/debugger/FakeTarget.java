package com.rox.roundabout.debugger;

import com.rox.roundabout.memory.ArrayMemory;
import com.rox.roundabout.memory.MemoryRegion;
import com.rox.roundabout.memory.MemorySource;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

/** A 4 KB target with two regions, one register, and a step on F that counts its runs. */
final class FakeTarget implements DebugTarget {
    final ArrayMemory memory = new ArrayMemory(0x1000);
    final List<MemoryRegion> regions = List.of(new MemoryRegion("Low", 0x000, 0x7FF, 0xFF5AA9FF),
            new MemoryRegion("High", 0x800, 0xFFF, 0xFF6FDC9C));
    boolean paused;
    int steps;

    @Override
    public MemorySource memory() {
        return memory;
    }

    @Override
    public List<MemoryRegion> regions() {
        return regions;
    }

    @Override
    public CpuState cpu() {
        return new CpuState(List.of(new Register("PC", 0x0123, 16)), Flag.fromBits("ZC", 1));
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    public List<Step> steps() {
        return List.of(new Step("Step", GLFW_KEY_F, () -> steps++));
    }

    @Override
    public String status() {
        return "fake";
    }
}
