package com.rox.ui.data;

/**
 * Side-effect-free view of a byte-addressable memory space.
 * Mirrors EmuRoxLab's {@code MemoryDebugSource} so an adapter is a one-liner.
 */
public interface MemorySource {
    int size();

    /** Reads a byte (0-255) without triggering hardware side effects. */
    int peek(int address);

    default boolean isEditable(int address) {
        return false;
    }

    default void poke(int address, int value) {
        throw new UnsupportedOperationException("read-only memory");
    }
}
