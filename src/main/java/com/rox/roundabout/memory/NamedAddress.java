package com.rox.roundabout.memory;

/** A named address, e.g. a game variable to watch from the start. */
public record NamedAddress(String name, int address) {
}
