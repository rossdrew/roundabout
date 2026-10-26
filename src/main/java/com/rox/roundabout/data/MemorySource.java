package com.rox.roundabout.data;

import java.util.function.IntUnaryOperator;

/**
 * Side-effect-free view of a byte-addressable memory space, e.g. an emulated CPU's bus. Reads must not
 * trigger hardware side effects (such as clearing a status register), since the debugger reads every byte
 * every frame.
 */
public interface MemorySource {
    int size();

    /** Reads a byte (0-255) without triggering hardware side effects. */
    int peek(int address);

    /**
     * A reader for one consistent pass over memory, taken once per UI frame. Override when a
     * snapshot is cheaper or more consistent than per-byte {@link #peek} calls.
     */
    default IntUnaryOperator reader() {
        return this::peek;
    }

    default boolean isEditable(int address) {
        return false;
    }

    default void poke(int address, int value) {
        throw new UnsupportedOperationException("read-only memory");
    }
}
