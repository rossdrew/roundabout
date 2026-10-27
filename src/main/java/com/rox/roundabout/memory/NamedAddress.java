package com.rox.roundabout.memory;

/**
 * A named address, e.g. a game variable to watch from the start.
 *
 * @param name    the watch's label
 * @param address the address to watch
 */
public record NamedAddress(String name, int address) {
}
