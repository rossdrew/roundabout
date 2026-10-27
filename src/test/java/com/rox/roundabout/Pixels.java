package com.rox.roundabout;

import static org.junit.jupiter.api.Assertions.fail;

/** Colour arithmetic and assertions for checking what {@link UiHarness} drew. */
public final class Pixels {
    /** Per-channel difference allowed for rounding in blending and antialiasing. */
    public static final int TOLERANCE = 3;

    private Pixels() {
    }

    /** {@code src} (ARGB, any alpha) blended over opaque {@code dst}, as Skia draws it. */
    public static int over(int dst, int src) {
        float a = (src >>> 24) / 255f;
        int r = Math.round(((src >> 16) & 0xFF) * a + ((dst >> 16) & 0xFF) * (1 - a));
        int g = Math.round(((src >> 8) & 0xFF) * a + ((dst >> 8) & 0xFF) * (1 - a));
        int b = Math.round((src & 0xFF) * a + (dst & 0xFF) * (1 - a));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** The largest difference between the colours' red, green and blue. */
    public static int distance(int a, int b) {
        return Math.max(Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)),
                Math.max(Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)), Math.abs((a & 0xFF) - (b & 0xFF))));
    }

    public static void assertColour(int expected, int actual, String message) {
        if (distance(expected, actual) > TOLERANCE) {
            fail(String.format("%s: expected #%08X but was #%08X", message, expected, actual));
        }
    }

    public static void assertNotColour(int unexpected, int actual, String message) {
        if (distance(unexpected, actual) <= TOLERANCE) {
            fail(String.format("%s: expected anything but #%08X", message, unexpected));
        }
    }
}
