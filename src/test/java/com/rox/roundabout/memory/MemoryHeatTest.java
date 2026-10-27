package com.rox.roundabout.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryHeatTest {
    private final int[] bytes = new int[16];
    private final MemorySource memory = new MemorySource() {
        public int size() {
            return bytes.length;
        }

        public int peek(int address) {
            return bytes[address];
        }
    };

    @Test
    void writesHeatUpThenDecayByHalfLife() {
        MemoryHeat heat = new MemoryHeat(memory, 1f);
        bytes[3] = 0x42;
        heat.update(0.016f);

        assertEquals(1f, heat.heat(3));
        assertEquals(0x42, heat.value(3));
        assertEquals(1, heat.writes(3));
        assertEquals(1, heat.changedLastFrame());
        assertEquals(0f, heat.heat(4));

        heat.update(1f);
        assertEquals(0.5f, heat.heat(3), 1e-4);
        assertEquals(0, heat.changedLastFrame());
    }

    @Test
    void startsFromTheCurrentContents() {
        bytes[5] = 9;
        MemoryHeat heat = new MemoryHeat(memory, 1f);
        heat.update(0.016f);

        assertEquals(9, heat.value(5));
        assertEquals(0, heat.changedLastFrame(), "existing contents aren't writes");
        assertEquals(0, heat.writes(5));
        assertEquals(16, heat.size());
    }

    @Test
    void heatHalvesEveryHalfLife() {
        MemoryHeat heat = new MemoryHeat(memory, 2f);
        bytes[0] = 1;
        heat.update(0);
        heat.update(2f);
        assertEquals(0.5f, heat.heat(0), 1e-4);
        heat.update(4f);
        assertEquals(0.125f, heat.heat(0), 1e-4);
    }
}
