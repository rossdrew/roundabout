package com.rox.roundabout.memory;

/** Writable memory over an array, for tests. */
public final class ArrayMemory implements MemorySource {
    public final int[] bytes;

    public ArrayMemory(int size) {
        bytes = new int[size];
    }

    @Override
    public int size() {
        return bytes.length;
    }

    @Override
    public int peek(int address) {
        return bytes[address];
    }

    @Override
    public boolean isEditable(int address) {
        return true;
    }

    @Override
    public void poke(int address, int value) {
        bytes[address] = value & 0xFF;
    }
}
