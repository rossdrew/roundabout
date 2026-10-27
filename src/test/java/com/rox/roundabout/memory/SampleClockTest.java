package com.rox.roundabout.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleClockTest {
    private static int samplesInOneSecond(int refreshHz) {
        SampleClock clock = new SampleClock(1 / 60f, 600);
        int samples = 0;
        for (int frame = 0; frame < refreshHz; frame++) {
            samples += clock.advance(1f / refreshHz);
        }
        return samples;
    }

    @Test
    void sampleRateDoesNotDependOnTheFrameRate() {
        for (int hz : new int[]{30, 60, 75, 144, 240}) {
            assertEquals(60, samplesInOneSecond(hz), 1, hz + " Hz");
        }
    }

    @Test
    void aLongStallIsCappedAndNotOwedAfterwards() {
        SampleClock clock = new SampleClock(0.1f, 5);

        assertEquals(5, clock.advance(10));
        assertEquals(0, clock.advance(0.05f));
        assertEquals(1, clock.advance(0.06f));
    }
}
