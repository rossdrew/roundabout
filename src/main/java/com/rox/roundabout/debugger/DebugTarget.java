package com.rox.roundabout.debugger;

import com.rox.roundabout.memory.AddressFormat;
import com.rox.roundabout.memory.MemoryRegion;
import com.rox.roundabout.memory.MemorySource;
import com.rox.roundabout.memory.NamedAddress;
import com.rox.roundabout.timing.FrameTimes;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

/** What {@link MemoryDebugger} inspects and controls - implemented by an emulator adapter. */
public interface DebugTarget {

    /**
     * The registers and flags to show, in display order.
     *
     * @param registers the registers; the first is highlighted (e.g. the PC)
     * @param flags     status flags, shown as chips below the registers
     */
    record CpuState(List<Register> registers, List<Flag> flags) {
        /** Copies both lists, so later changes to them don't show. */
        public CpuState {
            registers = List.copyOf(registers);
            flags = List.copyOf(flags);
        }
    }

    /**
     * A register shown as {@code bits / 4} hex digits (rounded up).
     *
     * @param name  its label, e.g. "PC"
     * @param value its contents; bits above {@code bits} are ignored
     * @param bits  its width, 1-64
     */
    record Register(String name, long value, int bits) {
        /**
         * Checks {@code bits}.
         *
         * @throws IllegalArgumentException if {@code bits} isn't 1-64
         */
        public Register {
            if (bits < 1 || bits > 64) {
                throw new IllegalArgumentException("bits must be 1-64: " + bits);
            }
        }

        /** The value in hex, without a prefix, e.g. {@code 00FF} for a 16-bit register. */
        public String hex() {
            long mask = bits == 64 ? -1L : (1L << bits) - 1;
            return String.format("%0" + (bits + 3) / 4 + "X", value & mask);
        }
    }

    /**
     * A status flag, lit when set.
     *
     * @param name its label, usually one letter
     * @param set  whether it's set
     */
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
     *
     * @param label the button's text, e.g. "Step frame"
     * @param key   the shortcut, a {@code GLFW_KEY_*} code
     * @param run   advances the target; called on the UI thread
     */
    record Step(String label, int key, Runnable run) {
        /** A step without a shortcut key. */
        public Step(String label, Runnable run) {
            this(label, GLFW_KEY_UNKNOWN, run);
        }
    }

    /** The memory to show. */
    MemorySource memory();

    /** Named address ranges, for colouring the hex view and the memory map; may be empty. */
    List<MemoryRegion> regions();

    /** How addresses and values are written; {@code 0x} prefixes unless overridden. */
    default AddressFormat.Style numberStyle() {
        return AddressFormat.Style.C;
    }

    /** Addresses to watch from the start; none by default. */
    default List<NamedAddress> labels() {
        return List.of();
    }

    /**
     * Frame streams to show in a frame-timing panel, e.g. frames produced and frames shown, each with its own
     * budget; none by default.
     */
    default List<FrameTimes> frameTimings() {
        return List.of();
    }

    /** The registers and flags now; read every frame. */
    CpuState cpu();

    /** True while stopped and the {@link #steps()} can run; read every frame. */
    boolean isPaused();

    /** Stops running; called on the UI thread. */
    void pause();

    /** Carries on running; called on the UI thread. */
    void resume();

    /** The ways to advance a paused target, in button order, e.g. one frame and one instruction; read every frame. */
    List<Step> steps();

    /** A short, human-readable run summary for the top bar, e.g. a frame count and speed. */
    String status();
}
