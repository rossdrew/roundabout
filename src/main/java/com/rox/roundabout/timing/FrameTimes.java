package com.rox.roundabout.timing;

import com.rox.roundabout.chart.RingSeries;

/**
 * Timestamps of a stream of events, such as frames, recorded on one producer thread and turned into
 * intervals (ms) on the UI thread. Recording is lock-free and allocation-free so it can sit on an
 * emulator's hot path; if the UI falls more than {@value #CAPACITY} events behind, the oldest are lost.
 */
public final class FrameTimes {
    private static final int CAPACITY = 1024;
    private static final int MASK = CAPACITY - 1;

    private final String name;
    private final float budgetMs;
    private final long[] stamps = new long[CAPACITY];
    private volatile long written;
    private long read;
    private long previous;
    private boolean hasPrevious;
    private final RingSeries intervalsMs;

    /**
     * @param history  how many intervals to keep, e.g. 600 for ten seconds at 60 fps
     * @param budgetMs the intended interval between events, e.g. 1000 / 60f for 60 fps or 20 for 50 Hz PAL
     */
    public FrameTimes(String name, int history, float budgetMs) {
        if (budgetMs <= 0) {
            throw new IllegalArgumentException("budgetMs must be positive: " + budgetMs);
        }
        this.name = name;
        this.budgetMs = budgetMs;
        intervalsMs = new RingSeries(history);
    }

    /** Producer thread: an event at {@code nanos} (a {@link System#nanoTime()} reading). */
    public void record(long nanos) {
        long w = written;
        stamps[(int) (w & MASK)] = nanos;
        written = w + 1;
    }

    /** UI thread, once per frame: moves events recorded since the last call into {@link #intervalsMs()}. */
    public void update() {
        long w = written;
        if (w - read > CAPACITY) {
            read = w - CAPACITY;
            hasPrevious = false; // the gap across lost events isn't a real interval
        }
        for (; read < w; read++) {
            long t = stamps[(int) (read & MASK)];
            if (hasPrevious) {
                intervalsMs.add((t - previous) / 1e6f);
            }
            previous = t;
            hasPrevious = true;
        }
    }

    public String name() {
        return name;
    }

    public float budgetMs() {
        return budgetMs;
    }

    /** Gaps between consecutive events, oldest first. UI thread only. */
    public RingSeries intervalsMs() {
        return intervalsMs;
    }
}
