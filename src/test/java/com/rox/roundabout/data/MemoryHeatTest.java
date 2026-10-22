package com.rox.roundabout.data;

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
}
