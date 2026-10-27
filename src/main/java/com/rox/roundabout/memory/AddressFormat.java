package com.rox.roundabout.memory;

/**
 * How addresses and byte values are written: hex addresses padded to the width of the address space
 * (an even number of digits), with the prefixes of a {@link Style}.
 *
 * @param digits how many hex digits an address has, 1-8
 * @param style  the prefixes
 */
public record AddressFormat(int digits, Style style) {

    /** Which prefixes mark hex and binary numbers. */
    public enum Style {
        /** {@code 0x1F}, {@code 0b00011111}: C, Java and most tools. */
        C("0x", "0b"),
        /** {@code $1F}, {@code %00011111}: Motorola and 6502 assemblers. */
        MOTOROLA("$", "%");

        private final String hex;
        private final String binary;

        Style(String hex, String binary) {
            this.hex = hex;
            this.binary = binary;
        }
    }

    /**
     * Checks {@code digits}.
     *
     * @throws IllegalArgumentException if {@code digits} isn't 1-8
     */
    public AddressFormat {
        if (digits < 1 || digits > 8) {
            throw new IllegalArgumentException("digits must be 1-8: " + digits);
        }
    }

    /** Enough digits for every address below {@code size}, e.g. 4 for 64 KB and 6 for 16 MB. */
    public static AddressFormat forSize(int size, Style style) {
        int digits = Integer.toHexString(Math.max(0, size - 1)).length();
        return new AddressFormat(Math.max(2, digits + digits % 2), style);
    }

    /** An address padded to {@link #digits()}, e.g. {@code 0x00FF}. */
    public String address(int address) {
        return style.hex + String.format("%0" + digits + "X", address);
    }

    /** A byte value as two hex digits, e.g. {@code 0x1F}. */
    public String value(int value) {
        return style.hex + String.format("%02X", value);
    }

    /** A byte value as eight binary digits, e.g. {@code 0b00011111}. */
    public String binary(int value) {
        return style.binary + String.format("%8s", Integer.toBinaryString(value & 0xFF)).replace(' ', '0');
    }
}
