package com.rox.ui.data;

import java.util.List;

/** A named, coloured address range (inclusive). */
public record MemoryRegion(String name, int start, int end, int colour) {

    public int size() {
        return end - start + 1;
    }

    public boolean contains(int address) {
        return address >= start && address <= end;
    }

    public static MemoryRegion find(List<MemoryRegion> regions, int address) {
        for (MemoryRegion r : regions) {
            if (r.contains(address)) {
                return r;
            }
        }
        return null;
    }
}
