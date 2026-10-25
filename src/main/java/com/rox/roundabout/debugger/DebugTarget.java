package com.rox.roundabout.debugger;

import com.rox.roundabout.data.AddressFormat;
import com.rox.roundabout.data.FrameTimes;
import com.rox.roundabout.data.MemoryRegion;
import com.rox.roundabout.data.MemorySource;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

/** What {@link MemoryDebugger} inspects and controls - implemented by an emulator adapter. */
public interface DebugTarget {

    /** The registers and flags to show, in display order; the first register is highlighted (e.g. the PC). */
    record CpuState(List<Register> registers, List<Flag> flags) {
        public CpuState {
            registers = List.copyOf(registers);
            flags = List.copyOf(flags);
        }
    }

    /** A register shown as {@code bits / 4} hex digits; bits above {@code bits} are ignored. */
    record Register(String name, long value, int bits) {
        public Register {
            if (bits < 1 || bits > 64) {
                throw new IllegalArgumentException("bits must be 1-64: " + bits);
            }
        }

        public String hex() {
            long mask = bits == 64 ? -1L : (1L << bits) - 1;
            return String.format("%0" + (bits + 3) / 4 + "X", value & mask);
        }
    }

    record Flag(String name, boolean set) {
        /**
         * One flag per character of {@code names}, most significant bit first, e.g. {@code fromBits("NV-BDIZC", p)}
         * for a 6502 status byte.
         */
        public static List<Flag> fromBits(String names, long value) {
            int n = names.length();
            List<Flag> flags = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                flags.add(new Flag(String.valueOf(names.charAt(i)), (value >>> (n - 1 - i) & 1) != 0));
            }
            return flags;
        }
    }

    /**
     * A run control shown as a button, with an optional shortcut key ({@code GLFW_KEY_*}, or
     * {@code GLFW_KEY_UNKNOWN} for none). Steps only run while paused.
     */
    record Step(String label, int key, Runnable run) {
        public Step(String label, Runnable run) {
            this(label, GLFW_KEY_UNKNOWN, run);
        }
    }

    /** A named address to watch from the start. */
    record Label(String name, int address) {
    }

    MemorySource memory();

    List<MemoryRegion> regions();

    /** How addresses and values are written; {@code 0x} prefixes unless overridden. */
    default AddressFormat.Style numberStyle() {
        return AddressFormat.Style.C;
    }

    default List<Label> labels() {
        return List.of();
    }

    /**
     * Frame streams to show in a frame-timing panel, e.g. frames produced and frames shown, each with its own
     * budget; none by default.
     */
    default List<FrameTimes> frameTimings() {
        return List.of();
    }

    CpuState cpu();

    boolean isPaused();

    void pause();

    void resume();

    /** The ways to advance a paused target, in button order, e.g. one frame and one instruction; read every frame. */
    List<Step> steps();

    /** A short, human-readable run summary for the top bar, e.g. a frame count and speed. */
    String status();
}
