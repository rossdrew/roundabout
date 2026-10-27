package com.rox.roundabout.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryRegionTest {
    private static final MemoryRegion ZERO_PAGE = new MemoryRegion("Zero page", 0x00, 0xFF, 0);
    private static final MemoryRegion STACK = new MemoryRegion("Stack", 0x100, 0x1FF, 0);

    @Test
    void aRegionIncludesBothEnds() {
        assertEquals(256, STACK.size());
        assertTrue(STACK.contains(0x100));
        assertTrue(STACK.contains(0x1FF));
        assertFalse(STACK.contains(0xFF));
        assertFalse(STACK.contains(0x200));
    }

    @Test
    void findReturnsTheRegionHoldingAnAddress() {
        List<MemoryRegion> regions = List.of(ZERO_PAGE, STACK);
        assertSame(ZERO_PAGE, MemoryRegion.find(regions, 0x10));
        assertSame(STACK, MemoryRegion.find(regions, 0x100));
        assertNull(MemoryRegion.find(regions, 0x200));
    }

    @Test
    void memoryIsReadOnlyByDefault() {
        MemorySource memory = new MemorySource() {
            public int size() {
                return 1;
            }

            public int peek(int address) {
                return 7;
            }
        };
        assertFalse(memory.isEditable(0));
        assertThrows(UnsupportedOperationException.class, () -> memory.poke(0, 1));
        assertEquals(7, memory.reader().applyAsInt(0));
    }
}
