package com.rox.roundabout.memory;

import java.util.function.IntUnaryOperator;

/**
 * Side-effect-free view of a byte-addressable memory space, e.g. an emulated CPU's bus. Reads must not
 * trigger hardware side effects (such as clearing a status register), since the debugger reads every byte
 * every frame.
 */
public interface MemorySource {
    /** The number of addresses; valid ones are {@code 0} to {@code size() - 1}. */
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

    /** True when {@link #poke} may write {@code address}; false by default. */
    default boolean isEditable(int address) {
        return false;
    }

    /** Writes a byte where {@link #isEditable} is true; throws {@link UnsupportedOperationException} by default. */
    default void poke(int address, int value) {
        throw new UnsupportedOperationException("read-only memory");
    }
}
