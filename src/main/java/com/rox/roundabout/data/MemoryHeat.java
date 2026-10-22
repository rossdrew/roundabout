package com.rox.roundabout.data;

import java.util.function.IntUnaryOperator;

/**
 * Detects writes by diffing a {@link MemorySource} once per UI frame and tracks a decaying
 * "heat" per address, plus a running write count. Works without any bus write hooks.
 */
public final class MemoryHeat {
    private final MemorySource memory;
    private final int[] values;
    private final float[] heat;
    private final int[] writes;
    private final float halfLifeSeconds;
    private int changedLastFrame;

    public MemoryHeat(MemorySource memory, float halfLifeSeconds) {
        this.memory = memory;
        this.halfLifeSeconds = halfLifeSeconds;
        values = new int[memory.size()];
        heat = new float[memory.size()];
        writes = new int[memory.size()];
        IntUnaryOperator read = memory.reader();
        for (int a = 0; a < values.length; a++) {
            values[a] = read.applyAsInt(a);
        }
    }

    public void update(float dtSeconds) {
        float decay = (float) Math.pow(0.5, dtSeconds / halfLifeSeconds);
        int changed = 0;
        IntUnaryOperator read = memory.reader();
        for (int a = 0; a < values.length; a++) {
            int v = read.applyAsInt(a);
            if (v != values[a]) {
                values[a] = v;
                heat[a] = 1f;
                writes[a]++;
                changed++;
            } else {
                heat[a] *= decay;
            }
        }
        changedLastFrame = changed;
    }

    public int size() {
        return values.length;
    }

    /** The value as of the last {@link #update}. */
    public int value(int address) {
        return values[address];
    }

    public float heat(int address) {
        return heat[address];
    }

    public int writes(int address) {
        return writes[address];
    }

    public int changedLastFrame() {
        return changedLastFrame;
    }
}
