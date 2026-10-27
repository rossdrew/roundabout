package com.rox.roundabout.memory;

import java.util.List;

/**
 * A named, coloured address range.
 *
 * @param name   e.g. "Zero page"
 * @param start  the first address
 * @param end    the last address (inclusive)
 * @param colour ARGB, for the region's cells and map band
 */
public record MemoryRegion(String name, int start, int end, int colour) {

    /** The number of addresses in the range. */
    public int size() {
        return end - start + 1;
    }

    /** True when {@code address} is in the range. */
    public boolean contains(int address) {
        return address >= start && address <= end;
    }

    /** The first region containing {@code address}, or null. */
    public static MemoryRegion find(List<MemoryRegion> regions, int address) {
        for (MemoryRegion r : regions) {
            if (r.contains(address)) {
                return r;
            }
        }
        return null;
    }
}
