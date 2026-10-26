package com.rox.roundabout.debugger;

/** Turns uneven frame times into a whole number of fixed-interval samples, so history covers real time. */
final class SampleClock {
    private final double interval;
    private final int maxPerAdvance;
    private double owed;

    /** @param maxPerAdvance the most samples one call returns; after a longer stall the excess is dropped */
    SampleClock(float intervalSeconds, int maxPerAdvance) {
        this.interval = intervalSeconds;
        this.maxPerAdvance = maxPerAdvance;
    }

    /** Adds {@code dtSeconds} of elapsed time and returns how many samples are now due. */
    int advance(float dtSeconds) {
        owed += Math.max(0, dtSeconds);
        int due = (int) (owed / interval);
        if (due > maxPerAdvance) {
            owed = 0;
            return maxPerAdvance;
        }
        owed -= due * interval;
        return due;
    }
}
